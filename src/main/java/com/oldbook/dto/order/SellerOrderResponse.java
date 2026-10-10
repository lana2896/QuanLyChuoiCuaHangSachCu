package com.oldbook.dto.order;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerOrderResponse {
    private Integer maDH;
    private LocalDateTime ngayTao;
    private String phuongThucTT;
    private String tenNguoiNhan;
    private String soDienThoaiNhan;
    private String diaChiGiao;
    private StoreOrderResponse storeOrder;
}
