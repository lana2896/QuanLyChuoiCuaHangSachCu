package com.oldbook.controller.branch;

import com.oldbook.dto.branch.BranchProvinceResponse;
import com.oldbook.dto.branch.BranchResponse;
import com.oldbook.dto.common.ApiResponse;
import com.oldbook.service.branch.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API công khai của Hệ thống chi nhánh (GET không cần đăng nhập, xem SecurityConfig). */
@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ApiResponse<List<BranchResponse>> list(@RequestParam(required = false) Integer maTinh,
                                                  @RequestParam(required = false) String q) {
        return ApiResponse.success(branchService.listPublic(maTinh, q));
    }

    @GetMapping("/provinces")
    public ApiResponse<List<BranchProvinceResponse>> provinces() {
        return ApiResponse.success(branchService.provinces());
    }

    @GetMapping("/{maCH}")
    public ApiResponse<BranchResponse> detail(@PathVariable Integer maCH) {
        return ApiResponse.success(branchService.getPublic(maCH));
    }
}
