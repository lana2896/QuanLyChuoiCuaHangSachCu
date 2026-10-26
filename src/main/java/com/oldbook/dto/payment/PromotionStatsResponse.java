package com.oldbook.dto.payment;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PromotionStatsResponse {
    private long tong;
    private long dangApDung;
    private long tamDung;
    private long hetHieuLuc;
}
