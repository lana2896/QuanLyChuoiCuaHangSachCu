package com.oldbook.service.store;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.store.ModerationSearchRequest;
import com.oldbook.dto.store.RejectRequest;
import com.oldbook.dto.store.StoreResponse;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.system.SystemLog;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.filter.auth.JwtAuthenticationFilter.AuthenticatedUserDetails;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.identity.AccountRepository;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.repository.system.SystemLogRepository;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.service.catalog.BookMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreModerationService {

    private final StoreRepository storeRepository;
    private final BookRepository bookRepository;
    private final AccountRepository accountRepository;
    private final SystemLogRepository systemLogRepository;
    private final StoreAccess storeAccess;
    private final EntityManager entityManager;

    public PageResponse<StoreResponse> searchStores(ModerationSearchRequest request) {
        storeAccess.requireModerator();
        validateSearch(request);
        Specification<Store> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("trangThaiDuyet"), request.getTrangThaiDuyet()));
            if (request.getMaCH() != null) {
                predicates.add(cb.equal(root.get("maCH"), request.getMaCH()));
            }
            if (hasKeyword(request)) {
                predicates.add(cb.like(cb.lower(root.get("tenCuaHang")), likeKeyword(request), '\\'));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.of(storeRepository.findAll(specification, pageRequest(request, "maCH"))
                .map(this::storeResponse));
    }

    public StoreResponse getStoreDetail(Integer maCH) {
        storeAccess.requireModerator();
        validateId(maCH, "Mã cửa hàng");
        return storeResponse(storeRepository.findById(maCH)
                .orElseThrow(() -> new BusinessException("Không tìm thấy cửa hàng")));
    }

    @Transactional
    public StoreResponse approveStore(Integer maCH) {
        AuthenticatedUserDetails actor = storeAccess.requireModerator();
        Store store = lockStore(maCH);
        requirePending(store.getTrangThaiDuyet(), "Cửa hàng");
        store.setTrangThaiDuyet(ApprovalStatus.DA_DUYET.name());
        storeRepository.save(store);
        audit(actor, "DUYET_CUA_HANG", "CUA_HANG", maCH, "Phê duyệt cửa hàng");
        return StoreResponse.from(store);
    }

    @Transactional
    public StoreResponse rejectStore(Integer maCH, RejectRequest request) {
        AuthenticatedUserDetails actor = storeAccess.requireModerator();
        String lyDo = rejectionReason(request);
        Store store = lockStore(maCH);
        requirePending(store.getTrangThaiDuyet(), "Cửa hàng");
        store.setTrangThaiDuyet(ApprovalStatus.TU_CHOI.name());
        storeRepository.save(store);
        // Cửa hàng chưa có cột lý do; nhật ký lưu lý do trong cùng giao dịch.
        audit(actor, "TU_CHOI_CUA_HANG", "CUA_HANG", maCH, lyDo);
        return StoreResponse.from(store).withLyDoTuChoi(lyDo);
    }

    public PageResponse<BookResponse> searchBooks(ModerationSearchRequest request) {
        storeAccess.requireModerator();
        validateSearch(request);
        Specification<Book> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("trangThaiDuyet"), request.getTrangThaiDuyet()));
            if (request.getMaCH() != null) {
                predicates.add(cb.equal(root.get("cuaHang").get("maCH"), request.getMaCH()));
            }
            if (hasKeyword(request)) {
                String keyword = likeKeyword(request);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("tenSach")), keyword, '\\'),
                        cb.like(cb.lower(root.get("tacGia")), keyword, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.of(bookRepository.findAll(specification, pageRequest(request, "maSach"))
                .map(BookMapper::toResponse));
    }

    public BookResponse getBookDetail(Integer maSach) {
        storeAccess.requireModerator();
        validateId(maSach, "Mã sách");
        return BookMapper.toResponse(bookRepository.findById(maSach)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sách")));
    }

    @Transactional
    public BookResponse approveBook(Integer maSach) {
        AuthenticatedUserDetails actor = storeAccess.requireModerator();
        Book book = lockBookAndStore(maSach);
        requirePending(book.getTrangThaiDuyet(), "Sách");
        if (!ApprovalStatus.DA_DUYET.name().equals(book.getCuaHang().getTrangThaiDuyet())) {
            throw new BusinessException("Cửa hàng phải được phê duyệt trước khi duyệt sách");
        }
        book.setTrangThaiDuyet(ApprovalStatus.DA_DUYET.name());
        book.setLyDoTuChoi(null);
        bookRepository.save(book);
        audit(actor, "DUYET_SACH", "SACH", maSach, "Phê duyệt sách");
        return BookMapper.toResponse(book);
    }

    @Transactional
    public BookResponse rejectBook(Integer maSach, RejectRequest request) {
        AuthenticatedUserDetails actor = storeAccess.requireModerator();
        String lyDo = rejectionReason(request);
        Book book = lockBookAndStore(maSach);
        requirePending(book.getTrangThaiDuyet(), "Sách");
        book.setTrangThaiDuyet(ApprovalStatus.TU_CHOI.name());
        book.setLyDoTuChoi(lyDo);
        bookRepository.save(book);
        audit(actor, "TU_CHOI_SACH", "SACH", maSach, lyDo);
        return BookMapper.toResponse(book);
    }

    private Store lockStore(Integer maCH) {
        validateId(maCH, "Mã cửa hàng");
        return storeRepository.findByIdForUpdate(maCH)
                .orElseThrow(() -> new BusinessException("Không tìm thấy cửa hàng"));
    }

    private Book lockBookAndStore(Integer maSach) {
        validateId(maSach, "Mã sách");
        Book current = bookRepository.findById(maSach)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sách"));
        if (current.getCuaHang() == null) {
            throw new BusinessException("Sách không thuộc cửa hàng hợp lệ");
        }
        Integer maCH = current.getCuaHang().getMaCH();
        // Thứ tự khóa thống nhất với quản lý sách/tồn kho: cửa hàng trước, sách sau.
        Store store = lockStore(maCH);
        Book locked = bookRepository.findByIdForUpdate(maSach)
                .orElseThrow(() -> new BusinessException("Không tìm thấy sách"));
        // Lần đọc tìm cửa hàng có thể đã đưa sách vào persistence context trước khi chờ khóa.
        entityManager.refresh(locked);
        if (locked.getCuaHang() == null || !maCH.equals(locked.getCuaHang().getMaCH())) {
            throw new BusinessException("Cửa hàng của sách đã thay đổi, vui lòng thử lại");
        }
        // Dùng chính trạng thái của cửa hàng đã được khóa để kiểm tra điều kiện duyệt.
        locked.setCuaHang(store);
        return locked;
    }

    private void audit(AuthenticatedUserDetails actor, String hanhDong,
                       String loaiDoiTuong, Integer maDoiTuong, String moTa) {
        systemLogRepository.save(SystemLog.builder()
                .taiKhoan(accountRepository.getReferenceById(actor.maTK()))
                .hanhDong(hanhDong)
                .loaiDoiTuong(loaiDoiTuong)
                .maDoiTuong(maDoiTuong)
                .moTa(moTa)
                .build());
    }

    private StoreResponse storeResponse(Store store) {
        StoreResponse response = StoreResponse.from(store);
        if (!ApprovalStatus.TU_CHOI.name().equals(store.getTrangThaiDuyet())) {
            return response;
        }
        String lyDo = systemLogRepository
                .findFirstByLoaiDoiTuongAndMaDoiTuongAndHanhDongOrderByThoiGianDescMaNhatKyDesc(
                        "CUA_HANG", store.getMaCH(), "TU_CHOI_CUA_HANG")
                .map(SystemLog::getMoTa)
                .orElse(null);
        return response.withLyDoTuChoi(lyDo);
    }

    private void requirePending(String trangThai, String doiTuong) {
        if (!ApprovalStatus.CHO_DUYET.name().equals(trangThai)) {
            throw new BusinessException(doiTuong + " không ở trạng thái chờ duyệt");
        }
    }

    private String rejectionReason(RejectRequest request) {
        if (request == null || request.lyDo() == null || request.lyDo().isBlank()) {
            throw new BusinessException("Lý do từ chối không được để trống");
        }
        if (request.lyDo().length() > 500) {
            throw new BusinessException("Lý do từ chối không được vượt quá 500 ký tự");
        }
        return request.lyDo().trim();
    }

    private void validateId(Integer value, String field) {
        if (value == null || value <= 0) {
            throw new BusinessException(field + " phải lớn hơn 0");
        }
    }

    private void validateSearch(ModerationSearchRequest request) {
        if (request == null || request.getPage() == null || request.getPage() < 0
                || request.getSize() == null || request.getSize() < 1 || request.getSize() > 100) {
            throw new BusinessException("Trang và kích thước trang không hợp lệ (tối đa 100)");
        }
        if ((long) request.getPage() * request.getSize() > Integer.MAX_VALUE) {
            throw new BusinessException("Trang yêu cầu vượt quá giới hạn phân trang");
        }
        if (request.getTrangThaiDuyet() == null
                || !List.of("CHO_DUYET", "DA_DUYET", "TU_CHOI").contains(request.getTrangThaiDuyet())) {
            throw new BusinessException("Trạng thái duyệt không hợp lệ");
        }
        if (request.getMaCH() != null) {
            validateId(request.getMaCH(), "Mã cửa hàng");
        }
        if (request.getTuKhoa() != null && request.getTuKhoa().length() > 255) {
            throw new BusinessException("Từ khóa không được vượt quá 255 ký tự");
        }
    }

    private boolean hasKeyword(ModerationSearchRequest request) {
        return request.getTuKhoa() != null && !request.getTuKhoa().isBlank();
    }

    private String likeKeyword(ModerationSearchRequest request) {
        String keyword = request.getTuKhoa().trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
                .replace("[", "\\[");
        return "%" + keyword + "%";
    }

    private PageRequest pageRequest(ModerationSearchRequest request, String idField) {
        return PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Order.asc("ngayTao"), Sort.Order.asc(idField)));
    }
}