package com.oldbook.constant.shipping;

import com.oldbook.constant.order.OrderStatus;

/**
 * Trạng thái giao hàng của một đơn hàng con, do đơn vị vận chuyển báo về.
 * Chỉ tồn tại sau khi chủ cửa hàng đã xác nhận đơn và đơn đã được chuyển cho đơn vị vận chuyển.
 */
public enum ShipmentStatus {

    /** Đơn vị vận chuyển đã tiếp nhận đơn, chưa phân công lấy hàng. */
    CHO_LAY_HANG("Chờ lấy hàng", OrderStatus.DA_XAC_NHAN),
    DANG_LAY_HANG("Đang lấy hàng", OrderStatus.DANG_LAY_HANG),
    DA_LAY_HANG("Đã lấy hàng", OrderStatus.DA_LAY_HANG),
    DANG_GIAO("Đang giao", OrderStatus.DANG_GIAO),
    DA_GIAO("Đã giao", OrderStatus.DA_GIAO),
    /** Một lần giao không thành công; đơn hàng con vẫn ở trạng thái "Đang giao" cho tới khi hoàn trả. */
    GIAO_THAT_BAI("Giao thất bại", OrderStatus.DANG_GIAO),
    DA_HOAN_TRA("Đã hoàn trả", OrderStatus.DA_HOAN_TRA);

    private final String tenHienThi;
    private final OrderStatus orderStatus;

    ShipmentStatus(String tenHienThi, OrderStatus orderStatus) {
        this.tenHienThi = tenHienThi;
        this.orderStatus = orderStatus;
    }

    public String getDisplayName() {
        return tenHienThi;
    }

    /** Trạng thái đơn hàng con tương ứng với trạng thái giao hàng này. */
    public OrderStatus toOrderStatus() {
        return orderStatus;
    }

    public boolean isFinal() {
        return this == DA_GIAO || this == DA_HOAN_TRA;
    }

    public boolean canMoveTo(ShipmentStatus next) {
        return switch (this) {
            case CHO_LAY_HANG -> next == DANG_LAY_HANG || next == DA_LAY_HANG || next == DANG_GIAO;
            case DANG_LAY_HANG -> next == DA_LAY_HANG || next == DANG_GIAO;
            case DA_LAY_HANG -> next == DANG_GIAO;
            case DANG_GIAO -> next == DA_GIAO || next == GIAO_THAT_BAI || next == DA_HOAN_TRA;
            case GIAO_THAT_BAI -> next == DANG_GIAO || next == DA_HOAN_TRA;
            case DA_GIAO, DA_HOAN_TRA -> false;
        };
    }

    public static ShipmentStatus fromName(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
