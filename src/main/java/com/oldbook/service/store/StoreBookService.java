package com.oldbook.service.store;

import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.dto.catalog.BookTitleResponse;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.catalog.BookTitle;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.BookImage;
import com.oldbook.repository.catalog.BookImageRepository;
import com.oldbook.repository.catalog.BookTitleRepository;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.service.catalog.BookMapper;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.dto.store.CreateBookRequest;
import com.oldbook.dto.store.UpdateBookRequest;
import com.oldbook.dto.store.UpdateStockRequest;
import com.oldbook.dto.store.UpdateSaleStatusRequest;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.security.store.StoreAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreBookService {
    private static final Set<String> SORT_FIELDS = Set.of("maSach", "tenSach", "giaBanCu", "soLuongTon", "ngayTao");

    private final StoreRepository storeRepository;
    private final BookRepository bookRepository;
    private final BookTitleRepository bookTitleRepository;
    private final BookImageRepository bookImageRepository;
    private final StoreAccess storeAccess;

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> listMyBooks(int page, int size, String sort, String trangThaiDuyet) {
        Integer maND = storeAccess.requireOwner();
        Store store = getMyStore(maND, false);
        PageRequest pageable = pageable(page, size, sort);
        String approval = parseApproval(trangThaiDuyet);
        Specification<Book> spec = (root, query, builder) -> {
            var owned = builder.equal(root.get("cuaHang").get("maCH"), store.getMaCH());
            return approval == null ? owned
                    : builder.and(owned, builder.equal(root.get("trangThaiDuyet"), approval));
        };
        var result = bookRepository.findAll(spec, pageable);
        Map<Integer, List<String>> images = imagesOf(result.getContent());
        return PageResponse.of(result.map(b -> BookMapper.toResponse(b, images.getOrDefault(b.getMaSach(), List.of()))));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookTitleResponse> searchTitles(String tuKhoa, Integer maDM, int page, int size) {
        storeAccess.requireOwner();
        if (page < 0 || size < 1 || size > 50) {
            throw new BusinessException("Trang phải từ 0 và kích thước trang phải từ 1 đến 50");
        }
        String keyword = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String selling = SaleStatus.DANG_BAN.name();
        Specification<BookTitle> spec = (root, query, cb) -> {
            var predicate = cb.and(cb.equal(root.get("trangThaiBan"), selling),
                    cb.equal(root.get("danhMuc").get("trangThaiBan"), selling));
            if (maDM != null) {
                predicate = cb.and(predicate, cb.equal(root.get("danhMuc").get("maDM"), maDM));
            }
            if (!keyword.isEmpty()) {
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("tenSach")), pattern, '\\'),
                        cb.like(cb.lower(root.get("tacGia")), pattern, '\\')));
            }
            return predicate;
        };
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "tenSach").and(Sort.by(Sort.Direction.ASC, "maTuaSach")));
        return PageResponse.of(bookTitleRepository.findAll(spec, pageable).map(BookTitleResponse::from));
    }

    @Transactional(readOnly = true)
    public BookResponse getMyBook(Integer maSach) {
        Integer maND = storeAccess.requireOwner();
        Store store = getMyStore(maND, false);
        Book book = getOwnedBook(maSach, store, false);
        return BookMapper.toResponse(book, imagesOf(List.of(book)).getOrDefault(book.getMaSach(), List.of()));
    }

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = getApprovedStoreForUpdate(maND);
        validateStock(request.soLuongTon());
        if (request.maTuaSach() == null || request.maTuaSach() <= 0) {
            throw new BusinessException("Tựa sách không hợp lệ");
        }
        BookTitle tuaSach = bookTitleRepository.findById(request.maTuaSach())
                .orElseThrow(() -> new BusinessException("Tựa sách không tồn tại"));
        if (!SaleStatus.DANG_BAN.name().equals(tuaSach.getTrangThaiBan())
                || !SaleStatus.DANG_BAN.name().equals(tuaSach.getDanhMuc().getTrangThaiBan())) {
            throw new BusinessException("Tựa sách hoặc danh mục này đã ngừng bán");
        }
        Book book = Book.builder().cuaHang(store).tuaSach(tuaSach).build();
        applyContent(book, new UpdateBookRequest(request.doMoiPercent(), request.tinhTrangVatLy(), request.hinhAnhUrls()));
        book.setSoLuongTon(request.soLuongTon());
        book.setTrangThaiDuyet(ApprovalStatus.CHO_DUYET.name());
        book.setTrangThaiBan(SaleStatus.DANG_BAN.name());
        Book saved = bookRepository.save(book);
        return withImages(saved, saveImages(saved, request.hinhAnhUrls()));
    }

    @Transactional
    public BookResponse update(Integer maSach, UpdateBookRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = getApprovedStoreForUpdate(maND);
        Book book = getOwnedBook(maSach, store, true);
        applyContent(book, request);
        book.setTrangThaiDuyet(ApprovalStatus.CHO_DUYET.name());
        book.setLyDoTuChoi(null);
        Book saved = bookRepository.save(book);
        return withImages(saved, saveImages(saved, request.hinhAnhUrls()));
    }

    @Transactional
    public BookResponse updateStock(Integer maSach, UpdateStockRequest request) {
        Integer maND = storeAccess.requireOwner();
        validateStock(request.soLuongTon());
        Store store = getApprovedStoreForUpdate(maND);
        Book book = getOwnedBook(maSach, store, true);
        book.setSoLuongTon(request.soLuongTon());
        Book saved = bookRepository.save(book);
        return withImages(saved, imagesOf(List.of(saved)).getOrDefault(saved.getMaSach(), List.of()));
    }

    @Transactional
    public BookResponse updateSaleStatus(Integer maSach, UpdateSaleStatusRequest request) {
        Integer maND = storeAccess.requireOwner();
        String status = request.trangThaiBan();
        if (!SaleStatus.DANG_BAN.name().equals(status) && !SaleStatus.NGUNG_BAN.name().equals(status)) {
            throw new BusinessException("Trạng thái bán phải là DANG_BAN hoặc NGUNG_BAN");
        }
        Store store = getApprovedStoreForUpdate(maND);
        Book book = getOwnedBook(maSach, store, true);
        if (SaleStatus.DANG_BAN.name().equals(status) && !book.tuaSachVaDanhMucDangBan()) {
            throw new BusinessException("Tựa sách hoặc danh mục này đang ngừng bán, chưa thể bật bán lại");
        }
        book.setTrangThaiBan(status);
        Book saved = bookRepository.save(book);
        return withImages(saved, imagesOf(List.of(saved)).getOrDefault(saved.getMaSach(), List.of()));
    }

    private Store getMyStore(Integer maND, boolean forUpdate) {
        return (forUpdate ? storeRepository.findByChuShopMaNDForUpdate(maND)
                : storeRepository.findByChuShop_MaND(maND))
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký cửa hàng"));
    }

    private Store getApprovedStoreForUpdate(Integer maND) {
        // All seller and moderation mutations lock the store before locking its book.
        Store store = getMyStore(maND, true);
        if (!ApprovalStatus.DA_DUYET.name().equals(store.getTrangThaiDuyet())) {
            throw new BusinessException("Cửa hàng phải được phê duyệt trước khi quản lý sản phẩm");
        }
        return store;
    }

    private Book getOwnedBook(Integer maSach, Store store, boolean forUpdate) {
        if (maSach == null || maSach <= 0) {
            throw new BusinessException("Mã sách không hợp lệ");
        }
        Book book = (forUpdate ? bookRepository.findByIdForUpdate(maSach) : bookRepository.findById(maSach))
                .orElseThrow(() -> new BusinessException("Sách không tồn tại"));
        if (book.getCuaHang() == null || !store.getMaCH().equals(book.getCuaHang().getMaCH())) {
            throw new AccessDeniedException("Bạn không có quyền quản lý sách của cửa hàng khác");
        }
        return book;
    }

    private Map<Integer, List<String>> imagesOf(List<Book> books) {
        if (books.isEmpty()) {
            return Map.of();
        }
        return bookImageRepository.findAllBySach_MaSachInOrderBySach_MaSachAscThuTuAsc(
                        books.stream().map(Book::getMaSach).toList()).stream()
                .collect(Collectors.groupingBy(i -> i.getSach().getMaSach(), LinkedHashMap::new,
                        Collectors.mapping(BookImage::getHinhAnhUrl, Collectors.toList())));
    }

    private BookResponse withImages(Book book, List<String> urls) {
        return BookMapper.toResponse(book, urls);
    }

    private List<String> saveImages(Book book, List<String> urls) {
        List<String> clean = validateImages(urls);
        bookImageRepository.deleteAllBySach_MaSach(book.getMaSach());
        bookImageRepository.flush();
        List<BookImage> rows = new java.util.ArrayList<>();
        for (int i = 0; i < clean.size(); i++) {
            rows.add(BookImage.builder().sach(book).hinhAnhUrl(clean.get(i)).thuTu(i).build());
        }
        bookImageRepository.saveAll(rows);
        return clean;
    }

    private List<String> validateImages(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            throw new BusinessException("Cần ít nhất 1 ảnh sách");
        }
        Set<String> seen = new HashSet<>();
        List<String> clean = new java.util.ArrayList<>();
        for (String url : urls) {
            String value = url == null ? "" : url.trim();
            if (!value.regionMatches(true, 0, "https://", 0, 8) || value.length() > 500) {
                throw new BusinessException("Đường dẫn ảnh không hợp lệ");
            }
            if (seen.add(value)) {
                clean.add(value);
            }
        }
        return clean;
    }

    private void applyContent(Book book, UpdateBookRequest request) {
        if (request.tinhTrangVatLy() == null || request.tinhTrangVatLy().isBlank()) {
            throw new BusinessException("Tình trạng thực tế không được để trống");
        }
        validateImages(request.hinhAnhUrls());
        if (request.doMoiPercent() == null || request.doMoiPercent().signum() <= 0
                || request.doMoiPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Độ mới phải lớn hơn 0 và không quá 100 phần trăm");
        }
        book.setDoMoiPercent(request.doMoiPercent());
        book.setTinhTrangVatLy(optionalText(request.tinhTrangVatLy()));
        book.tinhGiaBan();
    }

    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new BusinessException("Số lượng tồn phải là số nguyên không âm");
        }
    }

    private PageRequest pageable(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException("Trang phải từ 0 và kích thước trang phải từ 1 đến 100");
        }
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new BusinessException("Trang yêu cầu vượt quá giới hạn phân trang");
        }
        String[] parts = (sort == null || sort.isBlank() ? "ngayTao,desc" : sort).split(",", -1);
        if (parts.length > 2 || !SORT_FIELDS.contains(parts[0].trim())) {
            throw new BusinessException("Trường sắp xếp không hợp lệ");
        }
        String direction = parts.length == 1 ? "asc" : parts[1].trim().toLowerCase(Locale.ROOT);
        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            throw new BusinessException("Chiều sắp xếp phải là asc hoặc desc");
        }
        String field = "tenSach".equals(parts[0].trim()) ? "tuaSach.tenSach" : parts[0].trim();
        Sort order = Sort.by(Sort.Direction.fromString(direction), field);
        if (!"maSach".equals(parts[0].trim())) {
            order = order.and(Sort.by(Sort.Direction.DESC, "maSach"));
        }
        return PageRequest.of(page, size, order);
    }

    private String parseApproval(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ApprovalStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Trạng thái duyệt không hợp lệ");
        }
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
