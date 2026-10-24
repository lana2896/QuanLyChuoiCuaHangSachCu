package com.oldbook.service.store;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.dto.store.BookImageUploadResponse;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.service.media.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BookImageUploadService {
    private final StoreService storeService;
    private final ImageUploadService imageUploadService;

    public BookImageUploadResponse upload(MultipartFile file) {
        var store = storeService.getMyStore();
        if (!ApprovalStatus.DA_DUYET.name().equals(store.trangThaiDuyet())) {
            throw new BusinessException("Cửa hàng phải được phê duyệt trước khi quản lý sản phẩm");
        }
        return new BookImageUploadResponse(imageUploadService.upload(file, "oldbook/books"));
    }
}
