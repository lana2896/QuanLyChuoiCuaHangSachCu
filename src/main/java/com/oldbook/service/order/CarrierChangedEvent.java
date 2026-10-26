package com.oldbook.service.order;

/**
 * Phát ra khi cửa hàng xác nhận hoặc hủy một đơn đang giao cho một đơn vị vận chuyển.
 * Kênh đẩy (CarrierChangedPusher) nghe sự kiện này sau khi giao dịch commit để cổng của hãng tự tải lại.
 *
 * @param maCode mã hãng ở dạng chuẩn hóa (IN HOA), cùng dạng với mã trên URL /shipping/{maCode}
 */
public record CarrierChangedEvent(String maCode) {
}
