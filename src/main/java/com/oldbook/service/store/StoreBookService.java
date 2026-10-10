package com.oldbook.service.store;

import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.catalog.Category;
import com.oldbook.entity.catalog.Book;
import com.oldbook.repository.catalog.CategoryRepository;
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
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StoreBookService {
    private static final Set<String> SORT_FIELDS = Set.of("maSach", "tenSach", "giaBanCu", "soLuongTon", "ngayTao");

    private final StoreRepository storeRepository;
    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
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
        return PageResponse.of(bookRepository.findAll(spec, pageable).map(BookMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public BookResponse getMyBook(Integer maSach) {
        Integer maND = storeAccess.requireOwner();
        Store store = getMyStore(maND, false);
        return BookMapper.toResponse(getOwnedBook(maSach, store, false));
    }

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = getApprovedStoreForUpdate(maND);
        validateStock(request.soLuongTon());
        Book book = Book.builder().cuaHang(store).build();
        applyContent(book, new UpdateBookRequest(request.maDM(), request.tenSach(), request.tacGia(),
                request.nhaXuatBan(), request.namXuatBan(), request.giaBia(), request.giaBanCu(),
                request.doMoiPercent(), request.tinhTrangVatLy(), request.moTaChiTiet(), request.hinhAnhUrl()));
        book.setSoLuongTon(request.soLuongTon());
        book.setTrangThaiDuyet(ApprovalStatus.CHO_DUYET.name());
        book.setTrangThaiBan(SaleStatus.DANG_BAN.name());
        return BookMapper.toResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse update(Integer maSach, UpdateBookRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = getApprovedStoreForUpdate(maND);
        Book book = getOwnedBook(maSach, store, true);
        applyContent(book, request);
        book.setTrangThaiDuyet(ApprovalStatus.CHO_DUYET.name());
        book.setLyDoTuChoi(null);
        return BookMapper.toResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse updateStock(Integer maSach, UpdateStockRequest request) {
        Integer maND = storeAccess.requireOwner();
        validateStock(request.soLuongTon());
        Store store = getApprovedStoreForUpdate(maND);
        Book book = getOwnedBook(maSach, store, true);
        book.setSoLuongTon(request.soLuongTon());
        return BookMapper.toResponse(bookRepository.save(book));
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
        book.setTrangThaiBan(status);
        return BookMapper.toResponse(bookRepository.save(book));
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

    private void applyContent(Book book, UpdateBookRequest request) {
        if (request.maDM() == null || request.maDM() <= 0) {
            throw new BusinessException("Danh mục không hợp lệ");
        }
        if (request.tenSach() == null || request.tenSach().isBlank()) {
            throw new BusinessException("Tên sách không được để trống");
        }
        if (request.giaBanCu() == null || request.giaBanCu().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Giá bán phải lớn hơn 0");
        }
        if (request.giaBia() != null && request.giaBia().signum() < 0) {
            throw new BusinessException("Giá bìa không được âm");
        }
        if (request.doMoiPercent() != null && (request.doMoiPercent().signum() < 0
                || request.doMoiPercent().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BusinessException("Độ mới phải từ 0 đến 100 phần trăm");
        }
        Category category = categoryRepository.findById(request.maDM())
                .orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));
        book.setDanhMuc(category);
        book.setTenSach(request.tenSach().trim());
        book.setTacGia(optionalText(request.tacGia()));
        book.setNhaXuatBan(optionalText(request.nhaXuatBan()));
        book.setNamXuatBan(request.namXuatBan());
        book.setGiaBia(request.giaBia());
        book.setGiaBanCu(request.giaBanCu());
        book.setDoMoiPercent(request.doMoiPercent());
        book.setTinhTrangVatLy(optionalText(request.tinhTrangVatLy()));
        book.setMoTaChiTiet(optionalText(request.moTaChiTiet()));
        book.setHinhAnhUrl(optionalText(request.hinhAnhUrl()));
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
        Sort order = Sort.by(Sort.Direction.fromString(direction), parts[0].trim());
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
