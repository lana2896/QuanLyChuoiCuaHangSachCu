package com.oldbook.service.store;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.entity.catalog.Store;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.identity.Account;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.dto.store.CreateStoreRequest;
import com.oldbook.dto.store.StoreResponse;
import com.oldbook.dto.store.StoreRegistrationResponse;
import com.oldbook.dto.store.UpdateStoreRequest;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.service.notification.BusinessNotificationService;
import com.oldbook.repository.system.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final StoreAccess storeAccess;
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
     * Khách hàng tự đăng ký gian hàng: tạo thêm tài khoản Chủ cửa hàng cho chính người đó
     * và một cửa hàng chờ duyệt, trong cùng một giao dịch.
     * Tài khoản mới dùng được ngay để theo dõi trạng thái; việc đăng sách vẫn phải chờ quản lý duyệt cửa hàng.
     */
    @Transactional
    public StoreRegistrationResponse registerAsCustomer(CreateStoreRequest request) {
        Integer maND = storeAccess.requireCustomer();
        if (accountRepository.existsByNguoiDung_MaNDAndVaiTro(maND, Role.CHU_CUA_HANG.name())) {
            throw new BusinessException("Bạn đã có tài khoản chủ cửa hàng. Hãy chuyển sang tài khoản đó để quản lý gian hàng.");
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
        int soChiNhanh = storeRepository.findMaxSoChiNhanh() + 1;
        User owner = userRepository.findById(maND)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        Store store = Store.builder()
                .chuShop(owner)
                .soChiNhanh(soChiNhanh)
                .tenCuaHang("Oldbook chi nhánh " + soChiNhanh)
                .moTa(optionalText(request.moTa(), "Mô tả", 1000))
                .diaChiCH(requireText(request.diaChiCH(), "Địa chỉ cửa hàng", 500))
                .soDienThoaiCH(requireText(request.soDienThoaiCH(), "Số điện thoại cửa hàng", 20))
                .trangThaiDuyet(ApprovalStatus.CHO_DUYET.name())
                .build();
        Store daLuu = storeRepository.save(store);
        businessNotificationService.storePendingApproval(daLuu, false);
        return daLuu;
    }

    @Transactional
    public StoreResponse updateMyStore(UpdateStoreRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = storeRepository.findByChuShopMaNDForUpdate(maND)
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký cửa hàng"));
        store.setMoTa(optionalText(request.moTa(), "Mô tả", 1000));
        store.setDiaChiCH(requireText(request.diaChiCH(), "Địa chỉ cửa hàng", 500));
        store.setSoDienThoaiCH(requireText(request.soDienThoaiCH(), "Số điện thoại cửa hàng", 20));
        store.setTrangThaiDuyet(ApprovalStatus.CHO_DUYET.name());
        Store daLuu = storeRepository.save(store);
        businessNotificationService.storePendingApproval(daLuu, true);
        return StoreResponse.from(daLuu);
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
