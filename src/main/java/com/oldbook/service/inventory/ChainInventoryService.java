package com.oldbook.service.inventory;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.dto.inventory.ChainStockResponse;
import com.oldbook.entity.catalog.Book;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.security.store.StoreAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tồn kho toàn chuỗi cho Quản lý / Quản trị viên: tra một tựa sách để biết chi nhánh nào còn bao nhiêu cuốn,
 * từ đó điều phối điều chuyển giữa các chi nhánh.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChainInventoryService {

    private static final int MAX_COPIES = 500;
    private static final int MAX_TITLES = 50;

    private final BookRepository bookRepository;
    private final StoreAccess storeAccess;

    public List<ChainStockResponse> search(String tuKhoa) {
        storeAccess.requireModerator();
        String keyword = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT);
        if (keyword.length() > 255) {
            throw new BusinessException("Từ khóa tối đa 255 ký tự");
        }
        String pattern = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        List<Book> copies = bookRepository.findApprovedInStockByKeyword(
                ApprovalStatus.DA_DUYET.name(), pattern, PageRequest.of(0, MAX_COPIES));

        Map<Integer, List<Book>> byTitle = new LinkedHashMap<>();
        for (Book sach : copies) {
            byTitle.computeIfAbsent(sach.getTuaSach().getMaTuaSach(), key -> new ArrayList<>()).add(sach);
        }
        return byTitle.values().stream()
                .limit(MAX_TITLES)
                .map(this::toResponse)
                .toList();
    }

    private ChainStockResponse toResponse(List<Book> copies) {
        Book first = copies.get(0);
        List<ChainStockResponse.Item> items = copies.stream()
                .map(sach -> new ChainStockResponse.Item(
                        sach.getCuaHang().getMaCH(), sach.getCuaHang().getTenCuaHang(), sach.getMaSach(),
                        sach.getDoMoiPercent(), sach.getGiaBanCu(),
                        sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon(), sach.getTrangThaiBan()))
                .sorted((a, b) -> Integer.compare(b.soLuongTon(), a.soLuongTon()))
                .toList();
        long tong = items.stream().mapToLong(ChainStockResponse.Item::soLuongTon).sum();
        int soChiNhanh = (int) items.stream().map(ChainStockResponse.Item::maCH).distinct().count();
        return new ChainStockResponse(first.getTuaSach().getMaTuaSach(), first.getTenSach(), first.getTacGia(),
                tong, soChiNhanh, items);
    }
}
