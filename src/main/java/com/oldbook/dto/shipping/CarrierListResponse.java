package com.oldbook.dto.shipping;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CarrierListResponse {
    private CarrierStatsResponse thongKe;
    private List<CarrierDetailResponse> danhSach;
}
