package com.oldbook.service.store;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.entity.catalog.Store;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.entity.identity.User;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.dto.store.CreateStoreRequest;
import com.oldbook.dto.store.StoreResponse;
import com.oldbook.dto.store.UpdateStoreRequest;
import com.oldbook.repository.store.StoreRepository;
import com.oldbook.security.store.StoreAccess;
import com.oldbook.repository.system.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreService {
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreAccess storeAccess;
    private final SystemLogRepository systemLogRepository;

    @Transactional(readOnly = true)
    public StoreResponse getMyStore() {
        Integer maND = storeAccess.requireOwner();
        Store store = storeRepository.findByChuShop_MaND(maND)
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký cửa hàng"));
        StoreResponse response = StoreResponse.from(store);
        if (ApprovalStatus.TU_CHOI.name().equals(store.getTrangThaiDuyet())) {
            return systemLogRepository
                    .findFirstByLoaiDoiTuongAndMaDoiTuongAndHanhDongOrderByThoiGianDescMaNhatKyDesc(
                            "CUA_HANG", store.getMaCH(), "TU_CHOI_CUA_HANG")
                    .map(log -> response.withLyDoTuChoi(log.getMoTa()))
                    .orElse(response);
        }
        return response;
    }

    @Transactional
    public StoreResponse create(CreateStoreRequest request) {
        Integer maND = storeAccess.requireOwner();
        if (storeRepository.findByChuShop_MaND(maND).isPresent()) {
            throw new BusinessException("Bạn đã có cửa hàng");
        }
        String ten = requireText(request.tenCuaHang(), "Tên cửa hàng", 255);
        if (storeRepository.existsByTenCuaHangIgnoreCase(ten)) {
            throw new BusinessException("Tên cửa hàng đã được sử dụng");
        }
        User owner = userRepository.findById(maND)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
        Store store = Store.builder()
                .chuShop(owner)
                .tenCuaHang(ten)
                .moTa(optionalText(request.moTa(), "Mô tả", 1000))
                .diaChiCH(requireText(request.diaChiCH(), "Địa chỉ cửa hàng", 500))
                .soDienThoaiCH(requireText(request.soDienThoaiCH(), "Số điện thoại cửa hàng", 20))
                .trangThaiDuyet(ApprovalStatus.CHO_DUYET.name())
                .build();
        return StoreResponse.from(storeRepository.save(store));
    }

    @Transactional
    public StoreResponse updateMyStore(UpdateStoreRequest request) {
        Integer maND = storeAccess.requireOwner();
        Store store = storeRepository.findByChuShopMaNDForUpdate(maND)
                .orElseThrow(() -> new BusinessException("Bạn chưa đăng ký cửa hàng"));
        String ten = requireText(request.tenCuaHang(), "Tên cửa hàng", 255);
        if (storeRepository.existsByTenCuaHangIgnoreCaseAndMaCHNot(ten, store.getMaCH())) {
            throw new BusinessException("Tên cửa hàng đã được sử dụng");
        }
        store.setTenCuaHang(ten);
        store.setMoTa(optionalText(request.moTa(), "Mô tả", 1000));
        store.setDiaChiCH(requireText(request.diaChiCH(), "Địa chỉ cửa hàng", 500));
        store.setSoDienThoaiCH(requireText(request.soDienThoaiCH(), "Số điện thoại cửa hàng", 20));
        store.setTrangThaiDuyet(ApprovalStatus.CHO_DUYET.name());
        return StoreResponse.from(storeRepository.save(store));
    }

    private String requireText(String value, String field, int maxLength) {
        String normalized = optionalText(value, field, maxLength);
        if (normalized == null) {
            throw new BusinessException(field + " không được để trống");
        }
        return normalized;
    }

    private String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(field + " vượt quá " + maxLength + " ký tự");
        }
        return normalized;
    }
}
