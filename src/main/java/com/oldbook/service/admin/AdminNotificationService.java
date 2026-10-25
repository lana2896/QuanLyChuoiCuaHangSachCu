package com.oldbook.service.admin;

import com.oldbook.constant.auth.Role;
import com.oldbook.constant.notification.NotificationLevel;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.identity.Account;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.service.notification.NotificationService;
import com.oldbook.service.notification.NotificationService.NotificationContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Quyết định sự kiện tài khoản nào thì báo cho Quản trị viên, và ở mức độ nào.
 *
 * Quản trị viên chỉ cần biết những thay đổi chạm tới quyền quản trị:
 *
 *   Sự kiện       | liên quan Quản trị viên | liên quan Quản lý | vai trò khác
 *   tạo tài khoản | QUAN_TRONG              | THONG_TIN         | không báo
 *   khóa          | NGHIEM_TRONG            | THONG_TIN         | không báo
 *   mở khóa       | QUAN_TRONG              | THONG_TIN         | không báo
 *   thêm vai trò  | QUAN_TRONG              | THONG_TIN         | không báo
 *
 * Với thêm vai trò, "liên quan" tính theo vai trò vừa được thêm.
 * Việc khóa hay thêm vai trò khách hàng, chủ cửa hàng là vận hành thường ngày, vẫn xem được trong nhật ký.
 */
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private static final String QUAN_TRI_VIEN = Role.QUAN_TRI_VIEN.name();
    private static final String QUAN_LY = Role.QUAN_LY.name();

    private final NotificationService notificationService;
    private final AccountRepository accountRepository;

    @Transactional
    public void accountCreated(Integer maTKThucHien, Account taiKhoan) {

        NotificationLevel mucDo = mucDo(NotificationLevel.QUAN_TRONG, taiKhoan.getVaiTro());
        if (mucDo == null) {
            return;
        }

        gui(maTKThucHien, taiKhoan, "TAO_TAI_KHOAN", mucDo,
                "Tài khoản " + tenVaiTro(taiKhoan.getVaiTro()) + " mới được tạo",
                " đã tạo tài khoản #" + taiKhoan.getMaTK() + " cho " + ten(taiKhoan.getNguoiDung()) + ".");
    }

    @Transactional
    public void accountLocked(Integer maTKThucHien, Account taiKhoan, String lyDo) {

        NotificationLevel mucDo = mucDo(NotificationLevel.NGHIEM_TRONG, taiKhoan.getVaiTro());
        if (mucDo == null) {
            return;
        }

        gui(maTKThucHien, taiKhoan, "KHOA_TAI_KHOAN", mucDo,
                "Tài khoản " + tenVaiTro(taiKhoan.getVaiTro()) + " bị khóa",
                " đã khóa tài khoản #" + taiKhoan.getMaTK() + " của " + ten(taiKhoan.getNguoiDung())
                        + ". Lý do: " + lyDo);
    }

    @Transactional
    public void accountUnlocked(Integer maTKThucHien, Account taiKhoan) {

        NotificationLevel mucDo = mucDo(NotificationLevel.QUAN_TRONG, taiKhoan.getVaiTro());
        if (mucDo == null) {
            return;
        }

        gui(maTKThucHien, taiKhoan, "MO_KHOA_TAI_KHOAN", mucDo,
                "Tài khoản " + tenVaiTro(taiKhoan.getVaiTro()) + " được mở khóa",
                " đã mở khóa tài khoản #" + taiKhoan.getMaTK() + " của " + ten(taiKhoan.getNguoiDung()) + ".");
    }

    // taiKhoanMoi là tài khoản vừa được tạo cho vai trò mới của một người dùng đã có sẵn.
    @Transactional
    public void roleAdded(Integer maTKThucHien, Account taiKhoanMoi) {

        NotificationLevel mucDo = mucDo(NotificationLevel.QUAN_TRONG, taiKhoanMoi.getVaiTro());
        if (mucDo == null) {
            return;
        }

        gui(maTKThucHien, taiKhoanMoi, "THEM_VAI_TRO", mucDo,
                "Thêm vai trò " + tenVaiTro(taiKhoanMoi.getVaiTro()),
                " đã thêm vai trò " + tenVaiTro(taiKhoanMoi.getVaiTro()) + " cho " + ten(taiKhoanMoi.getNguoiDung())
                        + " (tài khoản mới #" + taiKhoanMoi.getMaTK() + ").");
    }

    // null nghĩa là không cần báo.
    private NotificationLevel mucDo(NotificationLevel khiLienQuanQuanTriVien, String... vaiTroLienQuan) {
        boolean coQuanLy = false;
        for (String vaiTro : vaiTroLienQuan) {
            if (QUAN_TRI_VIEN.equals(vaiTro)) {
                return khiLienQuanQuanTriVien;
            }
            coQuanLy = coQuanLy || QUAN_LY.equals(vaiTro);
        }
        return coQuanLy ? NotificationLevel.THONG_TIN : null;
    }

    private void gui(
            Integer maTKThucHien,
            Account taiKhoan,
            String loai,
            NotificationLevel mucDo,
            String tieuDe,
            String hanhDong
    ) {

        String nguoiThucHien = accountRepository.findById(maTKThucHien)
                .map(thucHien -> ten(thucHien.getNguoiDung()))
                .orElse("Một quản trị viên");

        // Trang Tài khoản & Vai trò tự lọc theo ?tuKhoa=, mở ra là thấy đúng tài khoản cần xem.
        String duongDan = "/admin/accounts?tuKhoa="
                + URLEncoder.encode(taiKhoan.getNguoiDung().getEmail(), StandardCharsets.UTF_8);

        notificationService.sendToRole(
                Role.QUAN_TRI_VIEN,
                maTKThucHien,
                new NotificationContent(loai, mucDo, tieuDe, nguoiThucHien + hanhDong, duongDan)
        );
    }

    private static String ten(User nguoiDung) {
        String hoTen = nguoiDung.getHoTen();
        return hoTen == null || hoTen.isBlank() ? nguoiDung.getEmail() : hoTen;
    }

    private static String tenVaiTro(String vaiTro) {
        try {
            return Role.valueOf(vaiTro).getDisplayName();
        } catch (IllegalArgumentException | NullPointerException ex) {
            return String.valueOf(vaiTro);
        }
    }
}
