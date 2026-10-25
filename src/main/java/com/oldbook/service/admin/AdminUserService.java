package com.oldbook.service.admin;

import com.oldbook.dto.admin.AdminUserProfileResponse;
import com.oldbook.dto.admin.AdminAuditLogResponse;
import com.oldbook.dto.identity.AdminAccountResponse;
import com.oldbook.dto.identity.AddressResponse;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.identity.Account;
import com.oldbook.entity.system.SystemLog;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.identity.AddressRepository;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.repository.system.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AddressRepository addressRepository;
    private final StoreRepository storeRepository;
    private final SystemLogRepository systemLogRepository;

    @Transactional(readOnly = true)
    public AdminUserProfileResponse getProfile(Integer maND) {

        validateMaND(maND);

        User nguoiDung = userRepository.findById(maND)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));

        return AdminUserProfileResponse.builder()
                .maND(nguoiDung.getMaND())
                .email(nguoiDung.getEmail())
                .hoTen(nguoiDung.getHoTen())
                .soDienThoai(nguoiDung.getSoDienThoai())
                .avatarUrl(nguoiDung.getAvatarUrl())
                .tieuSu(nguoiDung.getTieuSu())
                .ngayTao(nguoiDung.getNgayTao())
                .taiKhoans(accountRepository.findByNguoiDung_MaND(maND)
                        .stream()
                        .map(this::toAccountResponse)
                        .toList())
                .diaChis(addressRepository
                        .findAllByNguoiDung_MaNDOrderByLaMacDinhDescMaDiaChiDesc(maND)
                        .stream()
                        .map(this::toDiaChiResponse)
                        .toList())
                .cuaHang(storeRepository.findByChuShop_MaND(maND)
                        .map(com.oldbook.dto.store.StoreResponse::from)
                        .orElse(null))
                .hoatDongGanDay(systemLogRepository
                        .findTop20ByTaiKhoan_NguoiDung_MaNDOrderByThoiGianDescMaNhatKyDesc(maND)
                        .stream()
                        .map(this::toNhatKyResponse)
                        .toList())
                .build();
    }

    private AdminAccountResponse toAccountResponse(Account taiKhoan) {
        User nguoiDung = taiKhoan.getNguoiDung();

        return AdminAccountResponse.builder()
                .maTK(taiKhoan.getMaTK())
                .maND(nguoiDung.getMaND())
                .email(nguoiDung.getEmail())
                .hoTen(nguoiDung.getHoTen())
                .soDienThoai(nguoiDung.getSoDienThoai())
                .vaiTro(taiKhoan.getVaiTro())
                .trangThai(taiKhoan.getTrangThai())
                .ngayTao(taiKhoan.getNgayTao())
                .build();
    }

    private AddressResponse toDiaChiResponse(com.oldbook.entity.identity.Address diaChi) {
        return AddressResponse.builder()
                .maDiaChi(diaChi.getMaDiaChi())
                .tenNguoiNhan(diaChi.getTenNguoiNhan())
                .soDienThoai(diaChi.getSoDienThoai())
                .tinhThanh(diaChi.getTinhThanh())
                .quanHuyen(diaChi.getQuanHuyen())
                .phuongXa(diaChi.getPhuongXa())
                .diaChiChiTiet(diaChi.getDiaChiChiTiet())
                .laMacDinh(diaChi.getLaMacDinh())
                .build();
    }

    private AdminAuditLogResponse toNhatKyResponse(SystemLog log) {
        String vaiTro = log.getTaiKhoan() == null
                ? null
                : log.getTaiKhoan().getVaiTro();

        Integer maTK = log.getTaiKhoan() == null
                ? null
                : log.getTaiKhoan().getMaTK();

        return AdminAuditLogResponse.builder()
                .maNhatKy(log.getMaNhatKy())
                .maTK(maTK)
                .vaiTro(vaiTro)
                .hanhDong(log.getHanhDong())
                .loaiDoiTuong(log.getLoaiDoiTuong())
                .maDoiTuong(log.getMaDoiTuong())
                .moTa(log.getMoTa())
                .diaChiIp(log.getDiaChiIp())
                .thoiGian(log.getThoiGian())
                .build();
    }

    private void validateMaND(Integer maND) {
        if (maND == null || maND <= 0) {
            throw new BusinessException("Mã người dùng không hợp lệ");
        }
    }
}
