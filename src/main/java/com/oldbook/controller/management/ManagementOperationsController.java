package com.oldbook.controller.management;

import com.oldbook.dto.common.ApiResponse;
import com.oldbook.dto.management.ManagementDto.DoiHoaHongRequest;
import com.oldbook.dto.management.ManagementDto.DotChiRequest;
import com.oldbook.dto.management.ManagementDto.DotChiXacNhanRequest;
import com.oldbook.dto.management.ManagementDto.DotChiXemTruoc;
import com.oldbook.dto.management.ManagementDto.HoaHongDanhMuc;
import com.oldbook.dto.management.ManagementDto.HoaHongLichSu;
import com.oldbook.dto.management.ManagementDto.HuyHieu;
import com.oldbook.dto.management.ManagementDto.KetQuaNhac;
import com.oldbook.dto.management.ManagementDto.NhomChi;
import com.oldbook.dto.management.ManagementDto.TongQuan;
import com.oldbook.dto.management.StatsDto.ThongKe;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.service.management.PayoutManagementService;
import com.oldbook.service.management.CommissionManagementService;
import com.oldbook.service.management.ManagementOverviewService;
import com.oldbook.service.management.OperationsStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** API của cổng Quản lý: tổng quan, chi trả cho cửa hàng theo đợt, nhắc khai tài khoản, chiết khấu. Quyền do luật /api/management/** kiểm. */
@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
public class ManagementOperationsController {

    private final ManagementOverviewService managementOverviewService;
    private final PayoutManagementService payoutManagementService;
    private final CommissionManagementService commissionManagementService;
    private final OperationsStatsService thongKeService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<TongQuan>> tongQuan() {
        return ok(managementOverviewService.tongQuan());
    }

    @GetMapping("/overview/badges")
    public ResponseEntity<ApiResponse<HuyHieu>> huyHieu() {
        return ok(managementOverviewService.huyHieu());
    }

    // ---------------- Chi trả cho cửa hàng ----------------

    @GetMapping("/payments/store-payouts")
    public ResponseEntity<ApiResponse<List<NhomChi>>> chiTheoCuaHang(
            @RequestParam(required = false) String trangThai) {
        return ok(payoutManagementService.theoCuaHang(trangThai));
    }

    @PostMapping("/payments/store-payouts/preview")
    public ResponseEntity<ApiResponse<DotChiXemTruoc>> xemTruoc(@RequestBody DotChiRequest request) {
        return ok(payoutManagementService.xemTruoc(request.maYeuCau()));
    }

    @PostMapping("/payments/store-payouts/done")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> xacNhan(@RequestBody DotChiXacNhanRequest request) {
        int soKhoan = payoutManagementService.xacNhanDaChuyen(maTK(), request.maDotChi(), request.maYeuCau());
        return ok(Map.of("soKhoan", soKhoan));
    }

    @PostMapping("/payments/stores/{maCH}/remind-bank")
    public ResponseEntity<ApiResponse<KetQuaNhac>> nhacCuaHang(@PathVariable Integer maCH) {
        return ok(payoutManagementService.nhacCuaHang(maCH));
    }

    @PostMapping("/payments/refunds/{maYeuCau}/remind-bank")
    public ResponseEntity<ApiResponse<KetQuaNhac>> nhacKhach(@PathVariable Integer maYeuCau) {
        return ok(payoutManagementService.nhacKhachHoanTien(maYeuCau));
    }

    // ---------------- Thống kê ----------------

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ThongKe>> thongKe(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return ok(thongKeService.thongKe(tuNgay, denNgay));
    }

    @PostMapping("/stats/stores/{maCH}/remind-confirm")
    public ResponseEntity<ApiResponse<KetQuaNhac>> nhacXacNhan(@PathVariable Integer maCH) {
        return ok(thongKeService.nhacXacNhan(maCH));
    }

    // ---------------- Chiết khấu ----------------

    @GetMapping("/commissions")
    public ResponseEntity<ApiResponse<List<HoaHongDanhMuc>>> hoaHong() {
        return ok(commissionManagementService.danhSach());
    }

    @PutMapping("/commissions/{maDM}")
    public ResponseEntity<ApiResponse<HoaHongDanhMuc>> doiHoaHong(
            @PathVariable Integer maDM, @RequestBody DoiHoaHongRequest request) {
        return ok(commissionManagementService.doiTyLe(maTK(), maDM, request.tyLe(), request.ghiChu()));
    }

    @GetMapping("/commissions/{maDM}/history")
    public ResponseEntity<ApiResponse<List<HoaHongLichSu>>> lichSuHoaHong(@PathVariable Integer maDM) {
        return ok(commissionManagementService.lichSu(maDM));
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private static Integer maTK() {
        return ((AuthenticatedUserDetails) SecurityContextHolder.getContext().getAuthentication().getDetails()).maTK();
    }
}
