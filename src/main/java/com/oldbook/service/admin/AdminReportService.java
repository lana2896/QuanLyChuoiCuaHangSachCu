package com.oldbook.service.admin;

import com.oldbook.constant.order.OrderStatus;
import com.oldbook.dto.admin.AdminRevenueReportResponse;
import com.oldbook.dto.admin.AdminRevenueReportResponse.Point;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.admin.AdminReportRepository;
import com.oldbook.repository.admin.AdminReportRepository.DailySales;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    /*
     * Quy tắc doanh thu sàn (khớp OrderSettlementService):
     * - Đơn vị đếm là đơn của từng cửa hàng (StoreOrder), không phải đơn tổng.
     * - Chỉ tính đơn DA_GIAO. Đơn hủy và đơn hoàn trả không được tính;
     *   khi hoàn trả, hoa hồng của đơn cũng đã bị đưa về 0.
     * - GMV = tienHangCH (chưa trừ giảm giá, không gồm phí vận chuyển).
     * - Hoa hồng = commissionTien, được chốt lúc giao thành công. Đây là doanh thu của sàn.
     * - Mốc thời gian là ngayGiao.
     */
    private static final String DON_DA_GIAO = OrderStatus.DA_GIAO.name();

    private static final int SO_NGAY_MAC_DINH = 30;
    private static final int SO_NGAY_TOI_DA = 366;

    private final AdminReportRepository adminReportRepository;

    /**
     * Cả hai ngày đều tính vào kỳ. Bỏ trống cả hai thì lấy 30 ngày gần nhất, tính đến hôm nay.
     */
    @Transactional(readOnly = true)
    public AdminRevenueReportResponse getRevenue(LocalDate tuNgay, LocalDate denNgay) {

        LocalDate den = denNgay != null ? denNgay : LocalDate.now();
        LocalDate tu = tuNgay != null ? tuNgay : den.minusDays(SO_NGAY_MAC_DINH - 1L);

        if (tu.isAfter(den)) {
            throw new BusinessException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
        if (ChronoUnit.DAYS.between(tu, den) + 1 > SO_NGAY_TOI_DA) {
            throw new BusinessException("Kỳ báo cáo tối đa " + SO_NGAY_TOI_DA + " ngày.");
        }

        Map<LocalDate, DailySales> theoNgay = adminReportRepository
                .sumShopOrdersByDeliveryDay(
                        DON_DA_GIAO,
                        tu.atStartOfDay(),
                        den.plusDays(1).atStartOfDay()
                )
                .stream()
                .collect(Collectors.toMap(DailySales::getNgay, Function.identity()));

        List<Point> points = tu.datesUntil(den.plusDays(1))
                .map(ngay -> toPoint(ngay, theoNgay.get(ngay)))
                .toList();

        return AdminRevenueReportResponse.builder()
                .tuNgay(tu)
                .denNgay(den)
                .tongSoDon(points.stream().mapToLong(Point::soDon).sum())
                .tongGmv(points.stream().map(Point::gmv).reduce(BigDecimal.ZERO, BigDecimal::add))
                .tongHoaHong(points.stream().map(Point::hoaHong).reduce(BigDecimal.ZERO, BigDecimal::add))
                .points(points)
                .build();
    }

    private Point toPoint(LocalDate ngay, DailySales sales) {
        if (sales == null) {
            return new Point(ngay, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return new Point(ngay, sales.getSoDon(), nz(sales.getGmv()), nz(sales.getHoaHong()));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
