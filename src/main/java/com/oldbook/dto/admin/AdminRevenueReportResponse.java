package com.oldbook.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminRevenueReportResponse {

    private LocalDate tuNgay;

    private LocalDate denNgay;

    // Tổng của cả kỳ
    private long tongSoDon;

    private BigDecimal tongGmv;

    private BigDecimal tongHoaHong;

    // Đủ mọi ngày trong kỳ theo thứ tự tăng dần; ngày không có đơn mang giá trị 0.
    private List<Point> points;

    public record Point(
            LocalDate ngay,
            long soDon,
            BigDecimal gmv,
            BigDecimal hoaHong
    ) {
    }
}
