package com.oldbook.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminAuditLogSummaryResponse {

    private long hanhDongHomNay;

    private long khoaMoKhoaHomNay;

    private long phanQuyenHomNay;

    private long soIpGhiNhanHomNay;
}