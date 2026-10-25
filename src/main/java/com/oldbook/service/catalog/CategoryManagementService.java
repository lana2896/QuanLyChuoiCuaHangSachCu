package com.oldbook.service.catalog;

import com.oldbook.dto.catalog.CategoryRequest;
import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.dto.catalog.CategoryResponse;
import com.oldbook.dto.catalog.UpdateCatalogStatusRequest;
import com.oldbook.entity.catalog.Category;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.catalog.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll(Sort.by("tenDanhMuc").ascending()).stream()
                .map(CategoryManagementService::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String ten = request.tenDanhMuc().trim();
        if (categoryRepository.existsByTenDanhMucIgnoreCase(ten)) {
            throw new BusinessException("Tên danh mục đã tồn tại");
        }
        Category category = Category.builder().tenDanhMuc(ten).moTa(optional(request.moTa())).build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Integer maDM, CategoryRequest request) {
        Category category = find(maDM);
        String ten = request.tenDanhMuc().trim();
        if (categoryRepository.existsByTenDanhMucIgnoreCaseAndMaDMNot(ten, maDM)) {
            throw new BusinessException("Tên danh mục đã tồn tại");
        }
        category.setTenDanhMuc(ten);
        category.setMoTa(optional(request.moTa()));
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateSaleStatus(Integer maDM, UpdateCatalogStatusRequest request) {
        Category category = find(maDM);
        category.setTrangThaiBan(parseStatus(request.trangThaiBan()));
        return toResponse(categoryRepository.save(category));
    }

    private Category find(Integer maDM) {
        return categoryRepository.findById(maDM)
                .orElseThrow(() -> new BusinessException("Danh mục không tồn tại"));
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

    private static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getMaDM(), c.getTenDanhMuc(), c.getMoTa(), c.getTrangThaiBan());
    }
}
