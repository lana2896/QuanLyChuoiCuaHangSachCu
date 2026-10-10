package com.oldbook.dto.order;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEventResponse {
    private String trangThai;
    private String tenTrangThai;
    private String ghiChu;
    private LocalDateTime thoiGian;
}
