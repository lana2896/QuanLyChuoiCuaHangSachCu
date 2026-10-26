package com.oldbook.service.store;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.entity.catalog.Store;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.location.PhuongXa;
import com.oldbook.service.location.LocationService;
import com.oldbook.entity.identity.Account;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.dto.payment.BankAccountRequest;
import com.oldbook.dto.store.CreateStoreRequest;
import com.oldbook.dto.store.StoreResponse;
import com.oldbook.dto.store.StoreRegistrationResponse;
import com.oldbook.dto.store.UpdateStoreRequest;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.service.notification.BusinessNotificationService;
import com.oldbook.repository.system.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {
    /** Mọi cửa hàng là một chi nhánh của chuỗi; tên do hệ thống đặt, chủ chi nhánh không tự nhập. */
    static final String BRANCH_NAME_PREFIX = "Oldbook chi nhánh ";

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final StoreAccess storeAccess;
    private final LocationService locationService;
    private final SystemLogRepository systemLogRepository;
    private final BusinessNotificationService businessNotificationService;

    @Transactional(readOnly = true)
    public StoreResponse getMyStore() {
        Integer maND = storeAccess.requireOwner();
        Store store = storeRepository.findByChuShop_MaND(maND)
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký cửa hàng"));
        StoreResponse response = StoreResponse.from(store);
        if (ApprovalStatus.TU_CHOI.name().equals(store.getTrangThaiDuyet())) {
            return systemLogRepository
                    .findFirstByLoaiDoiTuongAndMaDoiTuongAndHanhDongOrderByThoiGianDescMaNhatKyDesc(
                            "CUA_HANG", store.getMaCH(), "TU_CHOI_CUA_HANG")
                    .map(log -> response.withLyDoTuChoi(log.getMoTa()))
                    .orElse(response);
        }
        return response;
    }

    @Transactional
    public StoreResponse create(CreateStoreRequest request) {
        Integer maND = storeAccess.requireOwner();
        return StoreResponse.from(createStore(maND, request));
    }

    /**
     * Khách hàng tự đăng ký mở chi nhánh: tạo thêm tài khoản Chủ chi nhánh cho chính người đó
     * và một cửa hàng chờ duyệt, trong cùng một giao dịch.
     * Tài khoản mới dùng được ngay để theo dõi trạng thái; việc đăng sách vẫn phải chờ quản lý duyệt cửa hàng.
     */
    @Transactional
    public StoreRegistrationResponse registerAsCustomer(CreateStoreRequest request) {
        Integer maND = storeAccess.requireCustomer();
        if (accountRepository.existsByNguoiDung_MaNDAndVaiTro(maND, Role.CHU_CUA_HANG.name())) {
            throw new BusinessException("Bạn đã có tài khoản chủ chi nhánh. Hãy chuyển sang tài khoản đó để quản lý chi nhánh.");
        }
        Store store = createStore(maND, request);
        Account taiKhoan = accountRepository.save(Account.builder()
                .nguoiDung(store.getChuShop())
                .vaiTro(Role.CHU_CUA_HANG.name())
                .trangThai(AccountStatus.HOAT_DONG.name())
                .build());
        return new StoreRegistrationResponse(taiKhoan.getMaTK(), StoreResponse.from(store));
    }

    private Store createStore(Integer maND, CreateStoreRequest request) {
        if (storeRepository.findByChuShop_MaND(maND).isPresent()) {
            throw new BusinessException("Bạn đã có cửa hàng");
        }
        PhuongXa xa = locationService.resolve(request.maTinh(), request.maXa());
        User owner = userRepository.findById(maND)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        // Số chi nhánh tăng dần theo toàn chuỗi; tên cửa hàng suy ra từ số này.
        int soChiNhanh = storeRepository.findMaxSoChiNhanh() + 1;
        Store store = Store.builder()
                .chuShop(owner)
                .soChiNhanh(soChiNhanh)
                .tenCuaHang(BRANCH_NAME_PREFIX + soChiNhanh)
                .moTa(optionalText(request.moTa(), "Mô tả", 1000))
                .tinh(xa.getTinh())
                .xa(xa)
                .diaChiChiTiet(requireText(request.diaChiChiTiet(), "Địa chỉ chi tiết", 500))
                .soDienThoaiCH(requireText(request.soDienThoaiCH(), "Số điện thoại cửa hàng", 20))
                .gioHoatDong(optionalText(request.gioHoatDong(), "Giờ hoạt động", 100))
                .trangThaiDuyet(ApprovalStatus.CHO_DUYET.name())
                .build();
        if (request.maNganHang() != null || request.soTaiKhoan() != null || request.tenChuTaiKhoan() != null) {
            assignBankAccount(store, request.maNganHang(), request.soTaiKhoan(), request.tenChuTaiKhoan());
        }
        Store daLuu;
        try {
            // Ghi ngay để ràng buộc unique của so_chi_nhanh / ten_cua_hang được kiểm tra trong lúc này:
            // hai người đăng ký cùng lúc có thể cùng lấy một số chi nhánh, người sau sẽ nhận lỗi rõ ràng thay vì lỗi 500.
            daLuu = storeRepository.saveAndFlush(store);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Số chi nhánh vừa được cấp cho cửa hàng khác. Vui lòng gửi đăng ký lại.");
        }
        businessNotificationService.storePendingApproval(daLuu, false);
        return daLuu;
    }

    @Transactional
    public StoreResponse updateMyStore(UpdateStoreRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = storeRepository.findByChuShopMaNDForUpdate(maND)
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký cửa hàng"));
        store.setMoTa(optionalText(request.moTa(), "Mô tả", 1000));
        PhuongXa xa = locationService.resolve(request.maTinh(), request.maXa());
        store.setTinh(xa.getTinh());
        store.setXa(xa);
        store.setDiaChiChiTiet(requireText(request.diaChiChiTiet(), "Địa chỉ chi tiết", 500));
        store.setSoDienThoaiCH(requireText(request.soDienThoaiCH(), "Số điện thoại cửa hàng", 20));
        store.setGioHoatDong(optionalText(request.gioHoatDong(), "Giờ hoạt động", 100));
        store.setTrangThaiDuyet(ApprovalStatus.CHO_DUYET.name());
        Store daLuu = storeRepository.save(store);
        businessNotificationService.storePendingApproval(daLuu, true);
        return StoreResponse.from(daLuu);
    }

    /**
     * Đổi tài khoản nhận tiền trụ sở chi. Khác với sửa thông tin cửa hàng, việc này không đưa chi nhánh
     * về trạng thái chờ duyệt: tài khoản ngân hàng không phải nội dung cần kiểm duyệt.
     */
    @Transactional
    public StoreResponse updateBankAccount(BankAccountRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = storeRepository.findByChuShopMaNDForUpdate(maND)
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký cửa hàng"));
        assignBankAccount(store, request.maNganHang(), request.soTaiKhoan(), request.tenChuTaiKhoan());
        return StoreResponse.from(storeRepository.save(store));
    }

    // Ba trường đi cùng nhau: thiếu một trường thì không chuyển khoản được.
    private void assignBankAccount(Store store, String maNganHang, String soTaiKhoan, String tenChuTaiKhoan) {
        store.setMaNganHang(requireText(maNganHang, "Ngân hàng", 20));
        store.setSoTaiKhoan(requireText(soTaiKhoan, "Số tài khoản", 50));
        store.setTenChuTaiKhoan(requireText(tenChuTaiKhoan, "Tên chủ tài khoản", 255));
    }

    private String requireText(String value, String field, int maxLength) {
        String normalized = optionalText(value, field, maxLength);
        if (normalized == null) {
            throw new BusinessException(field + " không được để trống");
        }
        return normalized;
    }

    private String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(field + " vượt quá " + maxLength + " ký tự");
        }
        return normalized;
    }
}
