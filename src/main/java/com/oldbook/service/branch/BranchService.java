package com.oldbook.service.branch;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.dto.branch.BranchProvinceResponse;
import com.oldbook.dto.branch.BranchResponse;
import com.oldbook.entity.catalog.Store;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hệ thống chi nhánh của chuỗi nhìn từ phía khách: danh sách chi nhánh đang hoạt động,
 * lọc theo tỉnh/thành, và số sách đang bán ở từng chi nhánh.
 * Chỉ chi nhánh đã được duyệt mới xuất hiện ở đây.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final StoreRepository storeRepository;
    private final BookRepository bookRepository;

    public List<BranchResponse> listPublic(Integer maTinh, String tuKhoa) {
        Map<Integer, long[]> stock = stockByBranch();
        String keyword = normalize(tuKhoa);
        return activeBranches().stream()
                .filter(store -> maTinh == null
                        || (store.getTinh() != null && maTinh.equals(store.getTinh().getMaTinh())))
                .map(store -> toResponse(store, stock))
                .filter(branch -> keyword.isEmpty()
                        || normalize(branch.tenCuaHang() + " " + branch.diaChiCH()).contains(keyword))
                .toList();
    }

    public BranchResponse getPublic(Integer maCH) {
        if (maCH == null || maCH < 1) {
            throw new BusinessException("Mã chi nhánh không hợp lệ");
        }
        Store store = storeRepository.findById(maCH)
                .filter(s -> ApprovalStatus.DA_DUYET.name().equals(s.getTrangThaiDuyet()))
                .orElseThrow(() -> new BusinessException("Chi nhánh không tồn tại hoặc chưa hoạt động"));
        return toResponse(store, stockByBranch());
    }

    public List<BranchProvinceResponse> provinces() {
        Map<Integer, BranchProvinceResponse> result = new LinkedHashMap<>();
        activeBranches().stream()
                .filter(store -> store.getTinh() != null)
                .sorted((a, b) -> a.getTinh().getTenTinh().compareToIgnoreCase(b.getTinh().getTenTinh()))
                .forEach(store -> result.merge(store.getTinh().getMaTinh(),
                        new BranchProvinceResponse(store.getTinh().getMaTinh(), store.getTinh().getTenTinh(), 1),
                        (old, add) -> new BranchProvinceResponse(old.maTinh(), old.tenTinh(), old.soChiNhanh() + 1)));
        return List.copyOf(result.values());
    }

    private List<Store> activeBranches() {
        return storeRepository.findAllByTrangThaiDuyetOrderBySoChiNhanhAsc(ApprovalStatus.DA_DUYET.name());
    }

    private BranchResponse toResponse(Store store, Map<Integer, long[]> stock) {
        long[] counts = stock.getOrDefault(store.getMaCH(), new long[]{0, 0});
        return BranchResponse.from(store, counts[0], counts[1]);
    }

    /** maCH -> {số bản sách đang bán, tổng số cuốn tồn}. */
    private Map<Integer, long[]> stockByBranch() {
        Map<Integer, long[]> result = new HashMap<>();
        for (Object[] row : bookRepository.summarizePublicStockByStore(
                ApprovalStatus.DA_DUYET.name(), SaleStatus.DANG_BAN.name())) {
            result.put((Integer) row[0], new long[]{toLong(row[1]), toLong(row[2])});
        }
        return result;
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static String normalize(String value) {
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
}
