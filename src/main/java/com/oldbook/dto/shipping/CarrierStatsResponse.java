package com.oldbook.dto.shipping;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CarrierStatsResponse {
    private long tongDoiTac;
    private long dangHopTac;
    private long tamDung;
    private long vanDonDangXuLy;
}
