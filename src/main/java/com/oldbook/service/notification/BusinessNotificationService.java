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
