package com.oldbook.service.notification;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.constant.notification.NotificationLevel;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.identity.Account;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.service.notification.NotificationService.NotificationContent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Quyết định sự kiện nghiệp vụ nào thì báo cho ai, với khách hàng, chủ cửa hàng và quản lý.
 * (Thông báo cho quản trị viên có quy tắc riêng trong AdminNotificationService.)
 *
 *   Sự kiện                            | Người nhận
 *   gian hàng đăng ký hoặc gửi duyệt lại | mọi Quản lý
 *   cửa hàng, sách được duyệt / bị từ chối | chủ cửa hàng đó
 *   đơn được xác nhận, đổi trạng thái giao | khách mua
 *   được thêm vai trò                     | các tài khoản đang có của người đó
 *
 * Các service khác chỉ gọi một dòng vào đây, không tự soạn nội dung thông báo.
 * Thông báo gắn với tài khoản: người có nhiều vai trò thấy thông báo của tài khoản đang dùng.
 */
@Service
@RequiredArgsConstructor
public class BusinessNotificationService {

    private static final String TRANG_KIEM_DUYET = "/store-moderation";
    private static final String TRANG_CUA_HANG = "/store/books";
    private static final String TRANG_DON_HANG = "/orders";
    private static final String TRANG_DON_CUA_HANG = "/store/orders";
    private static final String TRANG_HOAN_TIEN = "/payment/refunds";
    private static final String TRANG_QUAN_LY_THANH_TOAN = "/management/payments";

    private final NotificationService notificationService;
    private final AccountRepository accountRepository;

    @Transactional
    public void storePendingApproval(Store cuaHang, boolean guiLai) {
        notificationService.sendToRole(Role.QUAN_LY, null, new NotificationContent(
                "GIAN_HANG_CHO_DUYET",
                NotificationLevel.QUAN_TRONG,
                guiLai ? "Gian hàng gửi duyệt lại" : "Gian hàng mới chờ duyệt",
                "\"" + cuaHang.getTenCuaHang() + "\""
                        + (guiLai ? " vừa sửa thông tin và gửi duyệt lại." : " vừa đăng ký và đang chờ xét duyệt."),
                TRANG_KIEM_DUYET
        ));
    }

    @Transactional
    public void storeApproved(Store cuaHang) {
        sendToStoreOwner(cuaHang, new NotificationContent(
                "CUA_HANG_DUOC_DUYET",
                NotificationLevel.THONG_TIN,
                "Gian hàng đã được duyệt",
                "\"" + cuaHang.getTenCuaHang() + "\" đã được duyệt. Bạn có thể bắt đầu đăng sách.",
                TRANG_CUA_HANG
        ));
    }

    @Transactional
    public void storeRejected(Store cuaHang, String lyDo) {
        sendToStoreOwner(cuaHang, new NotificationContent(
                "CUA_HANG_BI_TU_CHOI",
                NotificationLevel.QUAN_TRONG,
                "Gian hàng chưa được duyệt",
                "\"" + cuaHang.getTenCuaHang() + "\" bị từ chối. Lý do: " + lyDo,
                TRANG_CUA_HANG
        ));
    }

    @Transactional
    public void bookApproved(Book sach) {
        sendToStoreOwner(sach.getCuaHang(), new NotificationContent(
                "SACH_DUOC_DUYET",
                NotificationLevel.THONG_TIN,
                "Sách đã được duyệt",
                "\"" + sach.getTenSach() + "\" đã được duyệt và hiển thị cho người mua.",
                TRANG_CUA_HANG
        ));
    }

    @Transactional
    public void bookRejected(Book sach, String lyDo) {
        sendToStoreOwner(sach.getCuaHang(), new NotificationContent(
                "SACH_BI_TU_CHOI",
                NotificationLevel.QUAN_TRONG,
                "Sách chưa được duyệt",
                "\"" + sach.getTenSach() + "\" bị từ chối. Lý do: " + lyDo,
                TRANG_CUA_HANG
        ));
    }

    @Transactional
    public void orderConfirmed(StoreOrder donHang) {
        sendToBuyer(donHang, new NotificationContent(
                "DON_HANG_DUOC_XAC_NHAN",
                NotificationLevel.THONG_TIN,
                "Đơn #" + donHang.getMaDHCH() + " đã được xác nhận",
                tenCuaHang(donHang) + " đã xác nhận đơn và đang chờ đơn vị vận chuyển tới lấy hàng.",
                TRANG_DON_HANG
        ));
    }

    @Transactional
    public void orderShippingStatusChanged(StoreOrder donHang, String tenTrangThai) {
        sendToBuyer(donHang, new NotificationContent(
                "DON_HANG_DOI_TRANG_THAI",
                NotificationLevel.THONG_TIN,
                "Đơn #" + donHang.getMaDHCH() + ": " + tenTrangThai,
                "Đơn hàng từ " + tenCuaHang(donHang) + " vừa chuyển sang trạng thái \"" + tenTrangThai + "\".",
                TRANG_DON_HANG
        ));
    }

    /**
     * Tài khoản mới chưa từng được đăng nhập, nên báo vào các tài khoản người đó đang dùng:
     * đó là nơi họ sẽ thấy và biết rằng mình có thể chuyển sang vai trò mới.
     */
    @Transactional
    public void roleAdded(Account taiKhoanMoi) {
        String tenVaiTro = Role.valueOf(taiKhoanMoi.getVaiTro()).getDisplayName();
        NotificationContent noiDung = new NotificationContent(
                "DUOC_THEM_VAI_TRO",
                NotificationLevel.QUAN_TRONG,
                "Bạn có thêm vai trò " + tenVaiTro,
                "Quản trị viên vừa thêm vai trò " + tenVaiTro
                        + " cho bạn. Mở menu tài khoản để chuyển sang vai trò này.",
                null
        );

        accountRepository
                .findByNguoiDung_MaNDAndTrangThai(
                        taiKhoanMoi.getNguoiDung().getMaND(),
                        AccountStatus.HOAT_DONG.name()
                )
                .stream()
                .filter(taiKhoan -> !taiKhoan.getMaTK().equals(taiKhoanMoi.getMaTK()))
                .forEach(taiKhoan -> notificationService.sendToAccount(taiKhoan, noiDung));
    }

    // ---------------------------------------------------------------
    // Thanh toán chuyển khoản (VietQR + Casso)
    // ---------------------------------------------------------------

    @Transactional
    public void paymentSucceeded(User khach, Integer maDH, BigDecimal soTien) {
        sendToAccountOf(khach, Role.KHACH_HANG, new NotificationContent(
                "THANH_TOAN_THANH_CONG",
                NotificationLevel.THONG_TIN,
                "Thanh toán thành công",
                "Đã nhận " + money(soTien) + ". Đơn hàng #" + maDH + " đã được tạo và chuyển cho cửa hàng.",
                TRANG_DON_HANG
        ));
    }

    @Transactional
    public void refundAccountNeeded(User khach, BigDecimal soTien, String lyDo) {
        sendToAccountOf(khach, Role.KHACH_HANG, new NotificationContent(
                "CAN_TAI_KHOAN_HOAN_TIEN",
                NotificationLevel.QUAN_TRONG,
                "Bạn có khoản hoàn tiền " + money(soTien),
                lyDo + " Vui lòng nhập tài khoản ngân hàng để nhận lại tiền.",
                TRANG_HOAN_TIEN
        ));
    }

    @Transactional
    public void refundCompleted(User khach, BigDecimal soTien) {
        sendToAccountOf(khach, Role.KHACH_HANG, new NotificationContent(
                "DA_HOAN_TIEN",
                NotificationLevel.THONG_TIN,
                "Đã hoàn " + money(soTien),
                "Sàn đã chuyển khoản hoàn tiền vào tài khoản bạn cung cấp.",
                TRANG_HOAN_TIEN
        ));
    }

    @Transactional
    public void transferPending(boolean hoanTien, BigDecimal soTien, String moTa) {
        notificationService.sendToRole(Role.QUAN_LY, null, new NotificationContent(
                hoanTien ? "CAN_HOAN_TIEN" : "CAN_CHI_CUA_HANG",
                NotificationLevel.QUAN_TRONG,
                (hoanTien ? "Cần hoàn tiền " : "Cần chi cho cửa hàng ") + money(soTien),
                moTa,
                TRANG_QUAN_LY_THANH_TOAN
        ));
    }

    @Transactional
    public void storePaidOut(Store cuaHang, Integer maDHCH, BigDecimal soTien) {
        sendToStoreOwner(cuaHang, new NotificationContent(
                "DA_CHI_CUA_HANG",
                NotificationLevel.THONG_TIN,
                "Sàn đã chi " + money(soTien),
                "Tiền của đơn #" + maDHCH + " (đã trừ hoa hồng) đã được chuyển vào tài khoản của chi nhánh.",
                TRANG_DON_CUA_HANG
        ));
    }

    @Transactional
    public void payoutAccountNeeded(Store cuaHang, Integer maDHCH) {
        sendToStoreOwner(cuaHang, new NotificationContent(
                "CAN_TAI_KHOAN_NHAN_TIEN",
                NotificationLevel.QUAN_TRONG,
                "Cần tài khoản ngân hàng để nhận tiền",
                "Đơn #" + maDHCH + " đã giao xong nhưng chi nhánh chưa khai tài khoản nhận tiền. "
                        + "Vui lòng bổ sung trong thông tin cửa hàng.",
                TRANG_CUA_HANG
        ));
    }

    private static String money(BigDecimal soTien) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
                .format(soTien == null ? BigDecimal.ZERO : soTien) + " đ";
    }

    private void sendToStoreOwner(Store cuaHang, NotificationContent noiDung) {
        if (cuaHang == null) {
            return;
        }
        sendToAccountOf(cuaHang.getChuShop(), Role.CHU_CUA_HANG, noiDung);
    }

    private void sendToBuyer(StoreOrder donHang, NotificationContent noiDung) {
        if (donHang.getDonHang() == null) {
            return;
        }
        sendToAccountOf(donHang.getDonHang().getNguoiMua(), Role.KHACH_HANG, noiDung);
    }

    // Người nhận không còn tài khoản hoạt động ở vai trò đó thì bỏ qua, không coi là lỗi.
    private void sendToAccountOf(User nguoiDung, Role vaiTro, NotificationContent noiDung) {
        if (nguoiDung == null) {
            return;
        }
        accountRepository
                .findByNguoiDung_MaNDAndVaiTro(nguoiDung.getMaND(), vaiTro.name())
                .filter(taiKhoan -> AccountStatus.HOAT_DONG.name().equals(taiKhoan.getTrangThai()))
                .ifPresent(taiKhoan -> notificationService.sendToAccount(taiKhoan, noiDung));
    }

    private static String tenCuaHang(StoreOrder donHang) {
        return donHang.getCuaHang() == null ? "Cửa hàng" : donHang.getCuaHang().getTenCuaHang();
    }
}
