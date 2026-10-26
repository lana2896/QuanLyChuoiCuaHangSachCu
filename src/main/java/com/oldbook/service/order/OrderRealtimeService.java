package com.oldbook.service.order;

import com.oldbook.constant.auth.Role;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.service.shipping.ShippingCarrierResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Báo cho khách mua và chủ chi nhánh biết một đơn hàng con vừa thay đổi, để trang theo dõi đơn của họ
 * tự cập nhật. Các service đơn hàng chỉ gọi một dòng vào đây, không tự lo chuyện socket.
 *
 * Phải gọi bên trong giao dịch đang sửa đơn: sự kiện chỉ được đẩy đi sau khi giao dịch đó commit,
 * nên trang tải lại luôn thấy dữ liệu mới và không bao giờ nhận tin về một thay đổi bị rollback.
 */
@Service
@RequiredArgsConstructor
public class OrderRealtimeService {

    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void notifyChanged(StoreOrder order) {
        if (order == null || order.getMaDHCH() == null) {
            return;
        }

        Order donHang = order.getDonHang();
        Store cuaHang = order.getCuaHang();

        // Thông báo gắn với tài khoản theo vai trò: khách và chủ chi nhánh là hai tài khoản khác nhau
        // ngay cả khi cùng một người, nên mỗi vai trò chỉ nhận tin ở trang của vai trò đó.
        Set<Integer> nguoiNhan = new LinkedHashSet<>();
        if (donHang != null) {
            themTaiKhoan(nguoiNhan, donHang.getNguoiMua(), Role.KHACH_HANG);
        }
        if (cuaHang != null) {
            themTaiKhoan(nguoiNhan, cuaHang.getChuShop(), Role.CHU_CUA_HANG);
        }
        if (nguoiNhan.isEmpty()) {
            return;
        }

        eventPublisher.publishEvent(new OrderChangedEvent(
                donHang == null ? null : donHang.getMaDH(),
                order.getMaDHCH(),
                Set.copyOf(nguoiNhan)
        ));
    }

    /**
     * Báo cho cổng của đơn vị vận chuyển biết danh sách đơn của hãng vừa đổi (cửa hàng xác nhận hoặc hủy đơn).
     * Cổng không đăng nhập nên có kênh riêng theo mã hãng. Cũng phải gọi bên trong giao dịch đang sửa đơn.
     */
    public void notifyCarrier(StoreOrder order) {
        ShippingCarrier carrier = order == null ? null : order.getDonViVanChuyen();
        String maCode = carrier == null ? null : ShippingCarrierResolver.normalize(carrier.getMaCode());
        if (maCode != null) {
            eventPublisher.publishEvent(new CarrierChangedEvent(maCode));
        }
    }

    private void themTaiKhoan(Set<Integer> nguoiNhan, User nguoiDung, Role vaiTro) {
        if (nguoiDung == null) {
            return;
        }
        accountRepository
                .findByNguoiDung_MaNDAndVaiTro(nguoiDung.getMaND(), vaiTro.name())
                .ifPresent(taiKhoan -> nguoiNhan.add(taiKhoan.getMaTK()));
    }
}
