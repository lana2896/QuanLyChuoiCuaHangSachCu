package com.oldbook.dto.payment;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PromotionListResponse {
    private PromotionStatsResponse thongKe;
    private List<PromotionResponse> danhSach;
}
