package com.oldbook.service.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oldbook.dto.auth.LoginResponse;
import com.oldbook.dto.identity.ProfileResponse;
import com.oldbook.dto.identity.UpdateProfileRequest;
import com.oldbook.entity.identity.User;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.identity.AccountRepository;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public ProfileResponse updateProfile(
            Integer maND,
            UpdateProfileRequest request
    ) {

        // 1. Tìm người dùng từ JWT
        User nguoiDung = userRepository.findById(maND)
                .orElseThrow(() ->
                        new BusinessException(
                                "Người dùng không tồn tại"
                        )
                );

        // 2. Chuẩn hóa họ tên
        String hoTen = request.getHoTen().trim();

        if (hoTen.isEmpty()) {
            throw new BusinessException(
                    "Họ tên không được để trống"
            );
        }

        // 3. Chuẩn hóa số điện thoại
        String soDienThoai = normalizePhone(
                request.getSoDienThoai()
        );

        // 4. Kiểm tra số điện thoại có thuộc người khác không
        if (soDienThoai != null
                && userRepository.existsBySoDienThoaiAndMaNDNot(
                soDienThoai,
                maND
        )) {

            throw new BusinessException(
                    "Số điện thoại đã được sử dụng"
            );
        }

        // 5. Cập nhật thông tin
        nguoiDung.setHoTen(hoTen);
        nguoiDung.setSoDienThoai(soDienThoai);

        String avatarUrl = request.getAvatarUrl();

        if (avatarUrl != null) {
            avatarUrl = avatarUrl.trim();

            if (avatarUrl.isEmpty()) {
                avatarUrl = null;
            }
        }

        nguoiDung.setAvatarUrl(avatarUrl);

        // 6. Chuẩn hóa tiểu sử
        String tieuSu = request.getTieuSu();

        if (tieuSu != null) {
            tieuSu = tieuSu.trim();

            if (tieuSu.isEmpty()) {
                tieuSu = null;
            }
        }

        nguoiDung.setTieuSu(tieuSu);

        // 7. Lưu
        userRepository.save(nguoiDung);

        // 8. Trả dữ liệu sau khi cập nhật
        return toProfileResponse(nguoiDung);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Integer maND) {

        User nguoiDung = userRepository.findById(maND)
                .orElseThrow(() ->
                        new BusinessException(
                                "Người dùng không tồn tại"
                        )
                );

        return toProfileResponse(nguoiDung);
    }

    private ProfileResponse toProfileResponse(
            User nguoiDung
    ) {
        return ProfileResponse.builder()
                .maND(nguoiDung.getMaND())
                .email(nguoiDung.getEmail())
                .hoTen(nguoiDung.getHoTen())
                .soDienThoai(nguoiDung.getSoDienThoai())
                .avatarUrl(nguoiDung.getAvatarUrl())
                .tieuSu(nguoiDung.getTieuSu())
                .taiKhoans(
                        accountRepository
                                .findByNguoiDung_MaND(nguoiDung.getMaND())
                                .stream()
                                .map(taiKhoan ->
                                        LoginResponse.AccountResponse.builder()
                                                .maTK(taiKhoan.getMaTK())
                                                .vaiTro(taiKhoan.getVaiTro())
                                                .trangThai(taiKhoan.getTrangThai())
                                                .build()
                                )
                                .toList()
                )
                .build();
    }

    private String normalizePhone(String soDienThoai) {

        if (soDienThoai == null) {
            return null;
        }

        String phone = soDienThoai.trim();

        if (phone.isEmpty()) {
            return null;
        }

        return phone;
    }
}