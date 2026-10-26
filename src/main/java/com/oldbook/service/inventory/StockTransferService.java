package com.oldbook.service.inventory;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.constant.inventory.StockTransferStatus;
import com.oldbook.dto.branch.BranchOptionResponse;
import com.oldbook.dto.inventory.CreateStockTransferRequest;
import com.oldbook.dto.inventory.StockTransferResponse;
import com.oldbook.dto.store.StoreResponse;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.BookImage;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.inventory.StockTransfer;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookImageRepository;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.inventory.StockTransferRepository;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.service.notification.BusinessNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Điều chuyển sách giữa các chi nhánh trong chuỗi.
 *
 *   Chi nhánh gửi lập phiếu   -> trừ tồn kho bản sách gửi, phiếu ở CHO_NHAN
 *   Chi nhánh nhận xác nhận   -> cộng vào bản cùng tựa, cùng độ mới, cùng tình trạng ở chi nhánh nhận
 *                                (chưa có thì tạo bản mới, kèm ảnh, đã duyệt sẵn vì sách đã qua kiểm duyệt ở chi nhánh gửi)
 *   Chi nhánh nhận từ chối    -> trả số cuốn về bản sách gửi
 *   Chi nhánh gửi hủy         -> trả số cuốn về bản sách gửi
 *
 * Thứ tự khóa giống StoreBookService: khóa chi nhánh của người thao tác trước, rồi phiếu, rồi bản sách.
 */
@Service
@RequiredArgsConstructor
public class StockTransferService {

    private static final int MANAGEMENT_LIMIT = 200;

    private final StockTransferRepository stockTransferRepository;
    private final StoreRepository storeRepository;
    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;
    private final StoreAccess storeAccess;
    private final BusinessNotificationService businessNotificationService;

    // ---------------------------------------------------------------- chi nhánh

    @Transactional(readOnly = true)
    public List<StockTransferResponse> listMine() {
        Store store = myStore(false);
        return stockTransferRepository.findAllOfStore(store.getMaCH()).stream()
                .map(phieu -> StockTransferResponse.from(phieu, store.getMaCH()))
                .toList();
    }

    /** Các chi nhánh khác đang hoạt động, để chọn nơi nhận sách. */
    @Transactional(readOnly = true)
    public List<BranchOptionResponse> destinations() {
        Store store = myStore(false);
        return storeRepository.findAllByTrangThaiDuyetOrderBySoChiNhanhAsc(ApprovalStatus.DA_DUYET.name()).stream()
                .filter(other -> !other.getMaCH().equals(store.getMaCH()))
                .map(other -> new BranchOptionResponse(other.getMaCH(), other.getTenCuaHang(),
                        StoreResponse.from(other).diaChiCH()))
                .toList();
    }

    @Transactional
    public StockTransferResponse create(CreateStockTransferRequest request) {
        Store nguon = myStore(true);
        requireActive(nguon, "Chi nhánh của bạn phải được duyệt trước khi điều chuyển sách");
        if (request.soLuong() == null || request.soLuong() < 1) {
            throw new BusinessException("Số lượng điều chuyển tối thiểu là 1");
        }
        if (request.maCHDich() == null || request.maCHDich().equals(nguon.getMaCH())) {
            throw new BusinessException("Hãy chọn một chi nhánh khác để nhận sách");
        }
        Store dich = storeRepository.findById(request.maCHDich())
                .orElseThrow(() -> new BusinessException("Chi nhánh nhận không tồn tại"));
        requireActive(dich, "Chi nhánh nhận chưa hoạt động, chưa thể chuyển sách tới");

        Book sach = lockBook(request.maSach());
        if (sach.getCuaHang() == null || !nguon.getMaCH().equals(sach.getCuaHang().getMaCH())) {
            throw new AccessDeniedException("Bạn chỉ điều chuyển được sách trong kho chi nhánh của mình");
        }
        if (!ApprovalStatus.DA_DUYET.name().equals(sach.getTrangThaiDuyet())) {
            throw new BusinessException("Chỉ điều chuyển được sách đã được duyệt");
        }
        int ton = sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon();
        if (request.soLuong() > ton) {
            throw new BusinessException("Kho chi nhánh chỉ còn " + ton + " cuốn của bản sách này");
        }
        sach.setSoLuongTon(ton - request.soLuong());
        bookRepository.save(sach);

        StockTransfer phieu = stockTransferRepository.save(StockTransfer.builder()
                .sachNguon(sach)
                .cuaHangNguon(nguon)
                .cuaHangDich(dich)
                .soLuong(request.soLuong())
                .trangThai(StockTransferStatus.CHO_NHAN.name())
                .ghiChu(optionalText(request.ghiChu()))
                .build());
        businessNotificationService.stockTransferIncoming(phieu);
        return StockTransferResponse.from(phieu, nguon.getMaCH());
    }

    @Transactional
    public StockTransferResponse accept(Integer maPhieu) {
        Store dich = myStore(true);
        requireActive(dich, "Chi nhánh của bạn phải được duyệt trước khi nhập sách");
        StockTransfer phieu = lockPending(maPhieu);
        requireReceiver(phieu, dich);

        Book nguon = phieu.getSachNguon();
        Book banNhan = findMatchingCopy(dich, nguon);
        if (banNhan != null) {
            Book locked = lockBook(banNhan.getMaSach());
            int ton = locked.getSoLuongTon() == null ? 0 : locked.getSoLuongTon();
            locked.setSoLuongTon(ton + phieu.getSoLuong());
            banNhan = bookRepository.save(locked);
        } else {
            banNhan = createCopy(dich, nguon, phieu.getSoLuong());
        }

        phieu.setSachDich(banNhan);
        phieu.setTrangThai(StockTransferStatus.DA_NHAN.name());
        phieu.setNgayXuLy(LocalDateTime.now());
        StockTransfer saved = stockTransferRepository.save(phieu);
        businessNotificationService.stockTransferAccepted(saved);
        return StockTransferResponse.from(saved, dich.getMaCH());
    }

    @Transactional
    public StockTransferResponse reject(Integer maPhieu, String lyDo) {
        Store dich = myStore(true);
        String reason = optionalText(lyDo);
        if (reason == null) {
            throw new BusinessException("Lý do từ chối không được để trống");
        }
        StockTransfer phieu = lockPending(maPhieu);
        requireReceiver(phieu, dich);
        returnToSource(phieu);
        phieu.setTrangThai(StockTransferStatus.TU_CHOI.name());
        phieu.setLyDoTuChoi(reason);
        phieu.setNgayXuLy(LocalDateTime.now());
        StockTransfer saved = stockTransferRepository.save(phieu);
        businessNotificationService.stockTransferRejected(saved);
        return StockTransferResponse.from(saved, dich.getMaCH());
    }

    @Transactional
    public StockTransferResponse cancel(Integer maPhieu) {
        Store nguon = myStore(true);
        StockTransfer phieu = lockPending(maPhieu);
        if (!nguon.getMaCH().equals(phieu.getCuaHangNguon().getMaCH())) {
            throw new AccessDeniedException("Chỉ chi nhánh gửi mới hủy được phiếu này");
        }
        returnToSource(phieu);
        phieu.setTrangThai(StockTransferStatus.DA_HUY.name());
        phieu.setNgayXuLy(LocalDateTime.now());
        StockTransfer saved = stockTransferRepository.save(phieu);
        businessNotificationService.stockTransferCancelled(saved);
        return StockTransferResponse.from(saved, nguon.getMaCH());
    }

    // ---------------------------------------------------------------- quản lý chuỗi

    @Transactional(readOnly = true)
    public List<StockTransferResponse> listForManagement(String trangThai) {
        storeAccess.requireModerator();
        PageRequest limit = PageRequest.of(0, MANAGEMENT_LIMIT);
        List<StockTransfer> rows;
        if (trangThai == null || trangThai.isBlank()) {
            rows = stockTransferRepository.findAllByOrderByNgayTaoDescMaPhieuDesc(limit);
        } else {
            StockTransferStatus status = StockTransferStatus.fromName(trangThai);
            if (status == null) {
                throw new BusinessException("Trạng thái phiếu điều chuyển không hợp lệ");
            }
            rows = stockTransferRepository.findAllByTrangThaiOrderByNgayTaoDescMaPhieuDesc(status.name(), limit);
        }
        return rows.stream().map(phieu -> StockTransferResponse.from(phieu, null)).toList();
    }

    // ---------------------------------------------------------------- nội bộ

    private Store myStore(boolean forUpdate) {
        Integer maND = storeAccess.requireOwner();
        return (forUpdate ? storeRepository.findByChuShopMaNDForUpdate(maND)
                : storeRepository.findByChuShop_MaND(maND))
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký chi nhánh"));
    }

    private void requireActive(Store store, String message) {
        if (!ApprovalStatus.DA_DUYET.name().equals(store.getTrangThaiDuyet())) {
            throw new BusinessException(message);
        }
    }

    private StockTransfer lockPending(Integer maPhieu) {
        if (maPhieu == null || maPhieu < 1) {
            throw new BusinessException("Mã phiếu điều chuyển không hợp lệ");
        }
        StockTransfer phieu = stockTransferRepository.findByIdForUpdate(maPhieu)
                .orElseThrow(() -> new BusinessException("Phiếu điều chuyển không tồn tại"));
        if (!StockTransferStatus.CHO_NHAN.name().equals(phieu.getTrangThai())) {
            throw new BusinessException("Phiếu điều chuyển này đã được xử lý");
        }
        return phieu;
    }

    private void requireReceiver(StockTransfer phieu, Store store) {
        if (!store.getMaCH().equals(phieu.getCuaHangDich().getMaCH())) {
            throw new AccessDeniedException("Phiếu điều chuyển này không gửi tới chi nhánh của bạn");
        }
    }

    private Book lockBook(Integer maSach) {
        if (maSach == null || maSach < 1) {
            throw new BusinessException("Mã sách không hợp lệ");
        }
        return bookRepository.findByIdForUpdate(maSach)
                .orElseThrow(() -> new BusinessException("Sách không tồn tại"));
    }

    private void returnToSource(StockTransfer phieu) {
        Book nguon = lockBook(phieu.getSachNguon().getMaSach());
        int ton = nguon.getSoLuongTon() == null ? 0 : nguon.getSoLuongTon();
        nguon.setSoLuongTon(ton + phieu.getSoLuong());
        bookRepository.save(nguon);
    }

    /** Bản sách ở chi nhánh nhận có cùng tựa, cùng độ mới và cùng mô tả tình trạng thì gộp tồn kho. */
    private Book findMatchingCopy(Store dich, Book nguon) {
        return bookRepository.findApprovedCopiesAtStore(dich.getMaCH(), nguon.getTuaSach().getMaTuaSach(),
                        ApprovalStatus.DA_DUYET.name()).stream()
                .filter(sach -> sach.getDoMoiPercent() != null && nguon.getDoMoiPercent() != null
                        && sach.getDoMoiPercent().compareTo(nguon.getDoMoiPercent()) == 0)
                .filter(sach -> Objects.equals(normalize(sach.getTinhTrangVatLy()), normalize(nguon.getTinhTrangVatLy())))
                .findFirst()
                .orElse(null);
    }

    private Book createCopy(Store dich, Book nguon, int soLuong) {
        Book moi = Book.builder()
                .cuaHang(dich)
                .tuaSach(nguon.getTuaSach())
                .doMoiPercent(nguon.getDoMoiPercent())
                .tinhTrangVatLy(nguon.getTinhTrangVatLy())
                .soLuongTon(soLuong)
                .trangThaiDuyet(ApprovalStatus.DA_DUYET.name())
                .trangThaiBan(nguon.tuaSachVaDanhMucDangBan() ? SaleStatus.DANG_BAN.name() : SaleStatus.NGUNG_BAN.name())
                .build();
        moi.tinhGiaBan();
        Book saved = bookRepository.save(moi);

        List<BookImage> anh = new ArrayList<>();
        List<BookImage> anhNguon = bookImageRepository.findAllBySach_MaSachInOrderBySach_MaSachAscThuTuAsc(
                List.of(nguon.getMaSach()));
        for (int i = 0; i < anhNguon.size(); i++) {
            anh.add(BookImage.builder().sach(saved).hinhAnhUrl(anhNguon.get(i).getHinhAnhUrl()).thuTu(i).build());
        }
        bookImageRepository.saveAll(anh);
        return saved;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
