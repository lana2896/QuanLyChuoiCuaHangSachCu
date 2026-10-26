package com.oldbook.service.notification;

import com.oldbook.constant.auth.AccountStatus;
import com.oldbook.constant.auth.Role;
import com.oldbook.constant.notification.NotificationLevel;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.inventory.StockTransfer;
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
 * Quyết định sự kiện nghiệp vụ nào thì báo cho ai, với khách hàng, chủ chi nhánh và quản lý.
 * (Thông báo cho quản trị viên có quy tắc riêng trong AdminNotificationService.)
 *
 *   Sự kiện                            | Người nhận
 *   chi nhánh đăng ký hoặc gửi duyệt lại | mọi Quản lý
 *   cửa hàng, sách được duyệt / bị từ chối | chủ chi nhánh đó
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
    private static final String TRANG_DIEU_CHUYEN = "/store/transfers";

    /** Loại thông báo nhắc khai tài khoản; PayoutManagementService dựa vào đây để giới hạn mỗi người một lần mỗi giờ. */
    public static final String LOAI_NHAC_KHAI_TAI_KHOAN = "NHAC_KHAI_TAI_KHOAN";
    public static final String LOAI_NHAC_XAC_NHAN_DON = "NHAC_XAC_NHAN_DON";

    private final NotificationService notificationService;
    private final AccountRepository accountRepository;

    @Transactional
    public void storePendingApproval(Store cuaHang, boolean guiLai) {
        notificationService.sendToRole(Role.QUAN_LY, null, new NotificationContent(
                "GIAN_HANG_CHO_DUYET",
                NotificationLevel.QUAN_TRONG,
                guiLai ? "Chi nhánh gửi duyệt lại" : "Chi nhánh mới chờ duyệt",
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
                "Chi nhánh đã được duyệt",
                "\"" + cuaHang.getTenCuaHang() + "\" đã được duyệt. Bạn có thể bắt đầu đăng sách.",
                TRANG_CUA_HANG
        ));
    }

    @Transactional
    public void storeRejected(Store cuaHang, String lyDo) {
        sendToStoreOwner(cuaHang, new NotificationContent(
                "CUA_HANG_BI_TU_CHOI",
                NotificationLevel.QUAN_TRONG,
                "Chi nhánh chưa được duyệt",
                "\"" + cuaHang.getTenCuaHang() + "\" bị từ chối. Lý do: " + lyDo,
                TRANG_CUA_HANG
        ));
    }

    @Transactional
    public void bookPendingApproval(Book sach, boolean guiLai) {
        String tenCuaHang = sach.getCuaHang() == null ? "một chi nhánh" : "\"" + sach.getCuaHang().getTenCuaHang() + "\"";
        notificationService.sendToRole(Role.QUAN_LY, null, new NotificationContent(
                "SACH_CHO_DUYET",
                NotificationLevel.QUAN_TRONG,
                guiLai ? "Sách chờ duyệt lại" : "Sách mới chờ phê duyệt",
                "\"" + sach.getTenSach() + "\" của " + tenCuaHang
                        + (guiLai ? " vừa được chỉnh sửa và đang chờ duyệt lại." : " vừa được đăng bán và đang chờ phê duyệt."),
                TRANG_KIEM_DUYET
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
    // Đặt đơn và hủy đơn
    // ---------------------------------------------------------------

    @Transactional
    public void newOrder(StoreOrder donHang) {
        sendToStoreOwner(donHang.getCuaHang(), new NotificationContent(
                "DON_HANG_MOI",
                NotificationLevel.QUAN_TRONG,
                "Có đơn hàng mới #" + donHang.getMaDHCH(),
                recipientName(donHang) + " vừa đặt đơn #" + donHang.getMaDHCH() + ". Vui lòng xác nhận đơn.",
                TRANG_DON_CUA_HANG
        ));
    }

    @Transactional
    public void customerCancelledOrder(StoreOrder donHang, String lyDo) {
        sendToStoreOwner(donHang.getCuaHang(), new NotificationContent(
                "DON_HANG_BI_KHACH_HUY",
                NotificationLevel.QUAN_TRONG,
                "Khách đã hủy đơn #" + donHang.getMaDHCH(),
                "Đơn #" + donHang.getMaDHCH() + " đã được hủy ngay vì chưa xác nhận. Lý do: " + lyDo,
                TRANG_DON_CUA_HANG
        ));
    }

    @Transactional
    public void customerRequestedCancel(StoreOrder donHang, String lyDo) {
        sendToStoreOwner(donHang.getCuaHang(), new NotificationContent(
                "YEU_CAU_HUY_DON",
                NotificationLevel.QUAN_TRONG,
                "Khách yêu cầu hủy đơn #" + donHang.getMaDHCH(),
                "Lý do: " + lyDo + ". Vui lòng đồng ý hoặc từ chối yêu cầu.",
                TRANG_DON_CUA_HANG
        ));
    }

    @Transactional
    public void storeCancelledOrder(StoreOrder donHang, String lyDo) {
        sendToBuyer(donHang, new NotificationContent(
                "DON_HANG_BI_CUA_HANG_HUY",
                NotificationLevel.QUAN_TRONG,
                "Đơn #" + donHang.getMaDHCH() + " đã bị hủy",
                tenCuaHang(donHang) + " đã hủy đơn. Lý do: " + lyDo,
                TRANG_DON_HANG
        ));
    }

    @Transactional
    public void cancelRequestApproved(StoreOrder donHang) {
        sendToBuyer(donHang, new NotificationContent(
                "YEU_CAU_HUY_DUOC_DUYET",
                NotificationLevel.THONG_TIN,
                "Đơn #" + donHang.getMaDHCH() + " đã được hủy",
                tenCuaHang(donHang) + " đã đồng ý yêu cầu hủy đơn của bạn.",
                TRANG_DON_HANG
        ));
    }

    @Transactional
    public void cancelRequestRejected(StoreOrder donHang, String lyDo) {
        sendToBuyer(donHang, new NotificationContent(
                "YEU_CAU_HUY_BI_TU_CHOI",
                NotificationLevel.QUAN_TRONG,
                "Yêu cầu hủy đơn #" + donHang.getMaDHCH() + " bị từ chối",
                tenCuaHang(donHang) + " đã từ chối yêu cầu hủy. Lý do: " + lyDo,
                TRANG_DON_HANG
        ));
    }

    private static String recipientName(StoreOrder donHang) {
        if (donHang.getDonHang() == null || donHang.getDonHang().getTenNguoiNhan() == null) {
            return "Khách hàng";
        }
        return donHang.getDonHang().getTenNguoiNhan();
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
                "Trụ sở đã chuyển khoản hoàn tiền vào tài khoản bạn cung cấp.",
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
                "Trụ sở đã chi " + money(soTien),
                "Tiền của đơn #" + maDHCH + " (đã trừ chiết khấu) đã được chuyển vào tài khoản của chi nhánh.",
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

    /** Giao dịch ngân hàng không khớp phiên nào, hoặc khớp nhưng không tạo được đơn: Quản lý cần xem. */
    @Transactional
    public void bankTransactionMismatch(BigDecimal soTien, String ketQua, String ghiChu) {
        notificationService.sendToRole(Role.QUAN_LY, null, new NotificationContent(
                "GIAO_DICH_LECH",
                NotificationLevel.QUAN_TRONG,
                "Giao dịch ngân hàng bị lệch " + money(soTien),
                ("CAN_HOAN_TIEN".equals(ketQua) ? "Tiền đã về nhưng không tạo được đơn" : "Tiền về không khớp phiên nào")
                        + (ghiChu == null ? "." : ": " + ghiChu),
                TRANG_QUAN_LY_THANH_TOAN + "?tab=transactions"
        ));
    }

    /** Quản lý nhắc chủ chi nhánh khai tài khoản nhận tiền. */
    @Transactional
    public void remindStoreBankAccount(Store cuaHang) {
        sendToStoreOwner(cuaHang, new NotificationContent(
                LOAI_NHAC_KHAI_TAI_KHOAN,
                NotificationLevel.QUAN_TRONG,
                "Vui lòng khai tài khoản nhận tiền",
                "Trụ sở đang giữ tiền bán hàng của \"" + cuaHang.getTenCuaHang()
                        + "\" nhưng chưa có tài khoản ngân hàng để chuyển. Hãy bổ sung trong thông tin cửa hàng.",
                TRANG_CUA_HANG
        ));
    }

    /** Quản lý nhắc cửa hàng xác nhận các đơn đang chờ quá lâu. */
    @Transactional
    public void remindConfirmOrders(Store cuaHang, long soDon) {
        sendToStoreOwner(cuaHang, new NotificationContent(
                LOAI_NHAC_XAC_NHAN_DON,
                NotificationLevel.QUAN_TRONG,
                "Có " + soDon + " đơn đang chờ bạn xác nhận",
                "Khách đã đặt hàng quá 48 giờ nhưng đơn chưa được xác nhận. Vui lòng xác nhận hoặc hủy đơn sớm.",
                TRANG_DON_CUA_HANG
        ));
    }

    /** Quản lý nhắc khách khai tài khoản nhận hoàn tiền. */
    @Transactional
    public void remindRefundAccount(User khach, BigDecimal soTien) {
        sendToAccountOf(khach, Role.KHACH_HANG, new NotificationContent(
                LOAI_NHAC_KHAI_TAI_KHOAN,
                NotificationLevel.QUAN_TRONG,
                "Vui lòng nhập tài khoản nhận hoàn " + money(soTien),
                "Trụ sở chưa thể hoàn tiền vì chưa có tài khoản ngân hàng của bạn. Hãy bổ sung ở mục Hoàn tiền của tôi.",
                TRANG_HOAN_TIEN
        ));
    }

    private static String money(BigDecimal soTien) {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
                .format(soTien == null ? BigDecimal.ZERO : soTien) + " đ";
    }

    // ---------------------------------------------------------------- điều chuyển kho giữa chi nhánh

    @Transactional
    public void stockTransferIncoming(StockTransfer phieu) {
        sendToStoreOwner(phieu.getCuaHangDich(), new NotificationContent(
                "DIEU_CHUYEN_DEN",
                NotificationLevel.QUAN_TRONG,
                "Có sách đang chuyển tới chi nhánh",
                "\"" + phieu.getCuaHangNguon().getTenCuaHang() + "\" chuyển " + phieu.getSoLuong() + " cuốn \""
                        + phieu.getSachNguon().getTenSach() + "\". Hãy kiểm hàng và xác nhận nhập kho.",
                TRANG_DIEU_CHUYEN
        ));
    }

    @Transactional
    public void stockTransferAccepted(StockTransfer phieu) {
        sendToStoreOwner(phieu.getCuaHangNguon(), new NotificationContent(
                "DIEU_CHUYEN_DA_NHAN",
                NotificationLevel.THONG_TIN,
                "Chi nhánh nhận đã nhập kho",
                "\"" + phieu.getCuaHangDich().getTenCuaHang() + "\" đã nhận " + phieu.getSoLuong() + " cuốn \""
                        + phieu.getSachNguon().getTenSach() + "\" (phiếu #" + phieu.getMaPhieu() + ").",
                TRANG_DIEU_CHUYEN
        ));
    }

    @Transactional
    public void stockTransferRejected(StockTransfer phieu) {
        sendToStoreOwner(phieu.getCuaHangNguon(), new NotificationContent(
                "DIEU_CHUYEN_BI_TU_CHOI",
                NotificationLevel.QUAN_TRONG,
                "Phiếu điều chuyển bị từ chối",
                "\"" + phieu.getCuaHangDich().getTenCuaHang() + "\" từ chối phiếu #" + phieu.getMaPhieu()
                        + ". " + phieu.getSoLuong() + " cuốn đã được trả lại kho của bạn. Lý do: " + phieu.getLyDoTuChoi(),
                TRANG_DIEU_CHUYEN
        ));
    }

    @Transactional
    public void stockTransferCancelled(StockTransfer phieu) {
        sendToStoreOwner(phieu.getCuaHangDich(), new NotificationContent(
                "DIEU_CHUYEN_DA_HUY",
                NotificationLevel.THONG_TIN,
                "Phiếu điều chuyển đã bị hủy",
                "\"" + phieu.getCuaHangNguon().getTenCuaHang() + "\" đã hủy phiếu #" + phieu.getMaPhieu()
                        + " chuyển \"" + phieu.getSachNguon().getTenSach() + "\".",
                TRANG_DIEU_CHUYEN
        ));
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
