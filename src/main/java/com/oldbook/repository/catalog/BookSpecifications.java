package com.oldbook.repository.catalog;

import com.oldbook.constant.catalog.TrangThaiBan;
import com.oldbook.constant.catalog.TrangThaiDuyet;
import com.oldbook.dto.catalog.SachSearchRequest;
import com.oldbook.entity.catalog.Sach;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class SachSpecifications {
    private SachSpecifications() {
    }

    public static Specification<Sach> publicVisible() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("trangThaiDuyet"), TrangThaiDuyet.DA_DUYET.name()),
                cb.equal(root.get("trangThaiBan"), TrangThaiBan.DANG_BAN.name()),
                cb.equal(root.join("cuaHang").get("trangThaiDuyet"), TrangThaiDuyet.DA_DUYET.name())
        );
    }

    public static Specification<Sach> search(SachSearchRequest request) {
        Specification<Sach> specification = publicVisible();
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
