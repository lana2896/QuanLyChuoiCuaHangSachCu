package com.oldbook.controller.catalog;

import com.oldbook.dto.catalog.SachResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.SachSearchRequest;
import com.oldbook.dto.catalog.DanhMucResponse;
import com.oldbook.service.catalog.SachService;
import com.oldbook.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sach")
public class SachController {

    private final SachService sachService;

    public SachController(SachService sachService) {
        this.sachService = sachService;
    }

    @GetMapping
    public ApiResponse<List<SachResponse>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.success(sachService.timSachCongKhai(q, category));
    }

    @GetMapping("/tim-kiem")
    public ApiResponse<PageResponse<SachResponse>> search(@ModelAttribute SachSearchRequest request) {
        return ApiResponse.success(sachService.search(request));
    }

    @GetMapping("/{maSach}")
    public ApiResponse<SachResponse> getDetail(@PathVariable Integer maSach) {
        return ApiResponse.success(sachService.getDetail(maSach));
    }

    @GetMapping("/danh-muc")
    public ApiResponse<List<DanhMucResponse>> getDanhMuc() {
        return ApiResponse.success(sachService.getDanhMuc());
    }
}
