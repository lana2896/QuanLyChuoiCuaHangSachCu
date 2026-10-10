package com.oldbook.service.catalog;

import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.dto.catalog.CategoryResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.dto.catalog.BookSearchRequest;
import com.oldbook.repository.catalog.CategoryRepository;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.catalog.BookSpecifications;
import com.oldbook.exception.common.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<BookResponse> searchPublicBooks(String keyword, String category) {
        String tuKhoa = normalize(keyword);
        String danhMuc = normalize(category);
        return bookRepository.findPublic(
                        ApprovalStatus.DA_DUYET.name(), SaleStatus.DANG_BAN.name())
                .stream()
                .filter(s -> danhMuc.isEmpty()
                        || normalize(s.getDanhMuc().getTenDanhMuc()).equals(danhMuc))
                .filter(s -> tuKhoa.isEmpty()
                        || normalize(s.getTenSach()).contains(tuKhoa)
                        || normalize(s.getTacGia()).contains(tuKhoa)
                        || normalize(s.getCuaHang().getTenCuaHang()).contains(tuKhoa))
                .map(BookMapper::toPublicResponse)
                .toList();
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public PageResponse<BookResponse> search(BookSearchRequest request) {
        validateSearch(request);
        var pageable = PageRequest.of(request.getPage(), request.getSize(), resolveSort(request.getSort()));
        return PageResponse.of(bookRepository.findAll(BookSpecifications.search(request), pageable)
                .map(BookMapper::toPublicResponse));
    }

    public BookResponse getDetail(Integer maSach) {
        if (maSach == null || maSach < 1) {
            throw new BusinessException("Mã sách phải là số nguyên dương");
        }
        return bookRepository.findOne(BookSpecifications.publicVisible()
                        .and((root, query, cb) -> cb.equal(root.get("maSach"), maSach)))
                .map(BookMapper::toPublicResponse)
                .orElseThrow(() -> new BusinessException("Sách không tồn tại hoặc chưa được công khai"));
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll(Sort.by("tenDanhMuc").ascending())
                .stream().map(dm -> new CategoryResponse(dm.getMaDM(), dm.getTenDanhMuc(), dm.getMoTa()))
                .toList();
    }

    private void validateSearch(BookSearchRequest request) {
        if (request.getPage() < 0 || request.getSize() < 1 || request.getSize() > 100) {
            throw new BusinessException("Trang phải từ 0, kích thước trang từ 1 đến 100");
        }
        if ((long) request.getPage() * request.getSize() > Integer.MAX_VALUE) {
            throw new BusinessException("Trang yêu cầu vượt quá giới hạn phân trang");
        }
        if ((request.getMaDM() != null && request.getMaDM() < 1)
                || (request.getMaCH() != null && request.getMaCH() < 1)) {
            throw new BusinessException("Mã danh mục và mã cửa hàng phải là số nguyên dương");
        }
        if (request.getTuKhoa() != null && request.getTuKhoa().length() > 255) {
            throw new BusinessException("Từ khóa tối đa 255 ký tự");
        }
        validateRange(request.getGiaTu(), request.getGiaDen(), null, "giá");
        validateRange(request.getDoMoiTu(), request.getDoMoiDen(), new BigDecimal("100"), "độ mới");
    }

    private void validateRange(BigDecimal lower, BigDecimal upper, BigDecimal maximum, String label) {
        if ((lower != null && (lower.signum() < 0 || (maximum != null && lower.compareTo(maximum) > 0)))
                || (upper != null && (upper.signum() < 0 || (maximum != null && upper.compareTo(maximum) > 0)))) {
            throw new BusinessException("Khoảng " + label + " không hợp lệ");
        }
        if (lower != null && upper != null && lower.compareTo(upper) > 0) {
            throw new BusinessException("Giá trị từ phải nhỏ hơn hoặc bằng giá trị đến trong khoảng " + label);
        }
    }

    private Sort resolveSort(String sort) {
        if (sort == null) {
            throw new BusinessException("Cách sắp xếp không hợp lệ");
        }
        Sort primary = switch (sort) {
            case "moi-nhat" -> Sort.by("ngayTao").descending();
            case "cu-nhat" -> Sort.by("ngayTao").ascending();
            case "gia-tang" -> Sort.by("giaBanCu").ascending();
            case "gia-giam" -> Sort.by("giaBanCu").descending();
            case "ten-az" -> Sort.by("tenSach").ascending();
            default -> throw new BusinessException("Cách sắp xếp không hợp lệ");
        };
        return primary.and(Sort.by("maSach").ascending());
    }
}
