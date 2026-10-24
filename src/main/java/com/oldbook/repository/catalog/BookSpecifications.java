package com.oldbook.repository.catalog;

import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.dto.catalog.BookSearchRequest;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.system.Review;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public final class BookSpecifications {
    private BookSpecifications() {
    }

    public static Specification<Book> publicVisible() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("trangThaiDuyet"), ApprovalStatus.DA_DUYET.name()),
                cb.equal(root.get("trangThaiBan"), SaleStatus.DANG_BAN.name()),
                cb.equal(root.join("cuaHang").get("trangThaiDuyet"), ApprovalStatus.DA_DUYET.name())
        );
    }

    public static Specification<Book> search(BookSearchRequest request) {
        Specification<Book> specification = publicVisible();
        if (request.getTuKhoa() != null && !request.getTuKhoa().isBlank()) {
            String pattern = "%" + escapeLike(request.getTuKhoa().trim().toLowerCase(Locale.ROOT)) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("tenSach")), pattern, '\\'),
                    cb.like(cb.lower(root.get("tacGia")), pattern, '\\'),
                    cb.like(cb.lower(root.get("nhaXuatBan")), pattern, '\\')
            ));
        }
        if (request.getMaDM() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("danhMuc").get("maDM"), request.getMaDM()));
        }
        if (request.getMaCH() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("cuaHang").get("maCH"), request.getMaCH()));
        }
        if (request.getGiaTu() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("giaBanCu"), request.getGiaTu()));
        }
        if (request.getGiaDen() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("giaBanCu"), request.getGiaDen()));
        }
        if (request.getDoMoiTu() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("doMoiPercent"), request.getDoMoiTu()));
        }
        if (request.getDoMoiDen() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("doMoiPercent"), request.getDoMoiDen()));
        }
        if (request.getSoSaoTu() != null) {
            specification = specification.and((root, query, cb) -> {
                Subquery<Double> averageRating = query.subquery(Double.class);
                Root<Review> review = averageRating.from(Review.class);
                averageRating.select(cb.avg(review.<Integer>get("soSao").cast(Double.class)));
                averageRating.where(
                        cb.equal(review.get("chiTietDonHang").get("sach").get("maSach"), root.get("maSach")),
                        cb.between(review.get("soSao"), 1, 5));
                return cb.greaterThanOrEqualTo(averageRating, request.getSoSaoTu().doubleValue());
            });
        }
        if (request.getTinhTrang() != null && !request.getTinhTrang().isEmpty()) {
            specification = specification.and((root, query, cb) -> cb.or(
                    request.getTinhTrang().stream().map(condition -> switch (condition) {
                        case "NHU_MOI" -> cb.between(root.get("doMoiPercent"),
                                new BigDecimal("90"), new BigDecimal("100"));
                        case "TOT" -> cb.and(
                                cb.greaterThanOrEqualTo(root.get("doMoiPercent"), new BigDecimal("70")),
                                cb.lessThan(root.get("doMoiPercent"), new BigDecimal("90")));
                        case "KHA" -> cb.and(
                                cb.greaterThanOrEqualTo(root.get("doMoiPercent"), BigDecimal.ZERO),
                                cb.lessThan(root.get("doMoiPercent"), new BigDecimal("70")));
                        default -> throw new IllegalArgumentException("Tình trạng sách không hợp lệ");
                    }).toArray(Predicate[]::new)));
        }
        if (request.getKhoangGia() != null && !request.getKhoangGia().isBlank()) {
            specification = specification.and((root, query, cb) -> switch (request.getKhoangGia()) {
                case "DUOI_50000" -> cb.lessThan(root.get("giaBanCu"), new BigDecimal("50000"));
                case "TU_50000_DEN_150000" -> cb.between(root.get("giaBanCu"),
                        new BigDecimal("50000"), new BigDecimal("150000"));
                case "TREN_150000" -> cb.greaterThan(root.get("giaBanCu"), new BigDecimal("150000"));
                default -> throw new IllegalArgumentException("Khoảng giá không hợp lệ");
            });
        }
        if (request.getConHang() != null) {
            specification = specification.and((root, query, cb) -> Boolean.TRUE.equals(request.getConHang())
                    ? cb.greaterThan(cb.coalesce(root.<Integer>get("soLuongTon"), 0), 0)
                    : cb.lessThanOrEqualTo(cb.coalesce(root.<Integer>get("soLuongTon"), 0), 0));
        }
        return specification;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[");
    }
}
