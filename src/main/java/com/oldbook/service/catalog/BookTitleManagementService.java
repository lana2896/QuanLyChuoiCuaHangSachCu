package com.oldbook.service.catalog;

import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.dto.catalog.BookTitleRequest;
import com.oldbook.dto.catalog.BookTitleResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.UpdateCatalogStatusRequest;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.BookTitle;
import com.oldbook.entity.catalog.Category;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.catalog.BookTitleRepository;
import com.oldbook.repository.catalog.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BookTitleManagementService {

    private final BookTitleRepository bookTitleRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public PageResponse<BookTitleResponse> search(String tuKhoa, Integer maDM, String trangThaiBan, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException("Trang phải từ 0 và kích thước trang phải từ 1 đến 100");
        }
        Specification<BookTitle> spec = (root, query, cb) -> cb.conjunction();
        if (maDM != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("danhMuc").get("maDM"), maDM));
        }
        if (trangThaiBan != null && !trangThaiBan.isBlank()) {
            String status = parseStatus(trangThaiBan);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("trangThaiBan"), status));
        }
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            String pattern = "%" + escapeLike(tuKhoa.trim().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("tenSach")), pattern, '\\'),
                    cb.like(cb.lower(root.get("tacGia")), pattern, '\\'),
                    cb.like(cb.lower(root.get("nhaXuatBan")), pattern, '\\')));
        }
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "tenSach").and(Sort.by(Sort.Direction.ASC, "maTuaSach")));
        return PageResponse.of(bookTitleRepository.findAll(spec, pageable).map(BookTitleResponse::from));
    }

    @Transactional(readOnly = true)
    public List<BookTitleResponse> listByCategory(Integer maDM) {
        return bookTitleRepository.findAllByDanhMuc_MaDMAndTrangThaiBanAndDanhMuc_TrangThaiBanOrderByTenSachAsc(
                maDM, SaleStatus.DANG_BAN.name(), SaleStatus.DANG_BAN.name()).stream()
                .map(BookTitleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookTitleResponse get(Integer maTuaSach) {
        return BookTitleResponse.from(find(maTuaSach));
    }

    @Transactional
    public BookTitleResponse create(BookTitleRequest request) {
        BookTitle title = BookTitle.builder().build();
        apply(title, request);
        return BookTitleResponse.from(bookTitleRepository.save(title));
    }

    @Transactional
    public BookTitleResponse update(Integer maTuaSach, BookTitleRequest request) {
        BookTitle title = find(maTuaSach);
        BigDecimal giaCu = title.getGiaGoc();
        apply(title, request);
        BookTitle saved = bookTitleRepository.save(title);
        if (giaCu.compareTo(saved.getGiaGoc()) != 0) {
            for (Book book : bookRepository.findAllByTuaSach_MaTuaSach(maTuaSach)) {
                book.tinhGiaBan();
            }
        }
        return BookTitleResponse.from(saved);
    }

    @Transactional
    public BookTitleResponse updateSaleStatus(Integer maTuaSach, UpdateCatalogStatusRequest request) {
        BookTitle title = find(maTuaSach);
        String status = parseStatus(request.trangThaiBan());
        if (SaleStatus.DANG_BAN.name().equals(status)
                && SaleStatus.NGUNG_BAN.name().equals(title.getDanhMuc().getTrangThaiBan())) {
            throw new BusinessException("Danh mục đang ngừng bán, hãy bán lại danh mục trước");
        }
        title.setTrangThaiBan(status);
        BookTitle saved = bookTitleRepository.save(title);
        bookRepository.updateSaleStatusByTitle(maTuaSach, status);
        return BookTitleResponse.from(saved);
    }

    private BookTitle find(Integer maTuaSach) {
        return bookTitleRepository.findById(maTuaSach)
                .orElseThrow(() -> new BusinessException("Tựa sách không tồn tại"));
    }

    private void apply(BookTitle title, BookTitleRequest request) {
        Category danhMuc = categoryRepository.findById(request.maDM())
                .orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));
        boolean doiDanhMuc = title.getDanhMuc() == null
                || !title.getDanhMuc().getMaDM().equals(danhMuc.getMaDM());
        if (doiDanhMuc && SaleStatus.NGUNG_BAN.name().equals(danhMuc.getTrangThaiBan())) {
            throw new BusinessException("Danh mục đã ngừng bán, không thể thêm hoặc chuyển tựa sách vào");
        }
        title.setDanhMuc(danhMuc);
        title.setTenSach(request.tenSach().trim());
        title.setTacGia(optional(request.tacGia()));
        title.setNhaXuatBan(optional(request.nhaXuatBan()));
        title.setNamXuatBan(request.namXuatBan());
        title.setGiaGoc(request.giaGoc());
        title.setMoTaChiTiet(optional(request.moTaChiTiet()));
        title.setHinhAnhUrl(optional(request.hinhAnhUrl()));
    }

    private static String parseStatus(String value) {
        if (!SaleStatus.DANG_BAN.name().equals(value) && !SaleStatus.NGUNG_BAN.name().equals(value)) {
            throw new BusinessException("Trạng thái bán phải là DANG_BAN hoặc NGUNG_BAN");
        }
        return value;
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[");
    }
}
