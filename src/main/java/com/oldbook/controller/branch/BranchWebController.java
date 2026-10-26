package com.oldbook.controller.branch;

import com.oldbook.dto.branch.BranchResponse;
import com.oldbook.dto.catalog.BookSearchRequest;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.service.branch.BranchService;
import com.oldbook.service.catalog.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/** Trang Hệ thống chi nhánh và trang chủ từng chi nhánh, vẽ phía máy chủ. */
@Controller
@RequiredArgsConstructor
public class BranchWebController {

    private static final int BOOK_PAGE_SIZE = 12;

    private final BranchService branchService;
    private final BookService bookService;

    @GetMapping("/branches")
    public String list(@RequestParam(required = false) Integer tinh,
                       @RequestParam(required = false) String q,
                       Model model) {
        List<BranchResponse> branches = branchService.listPublic(tinh, q);
        model.addAttribute("branches", branches);
        model.addAttribute("provinces", branchService.provinces());
        model.addAttribute("selectedTinh", tinh);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("tongSach", branches.stream().mapToLong(BranchResponse::tongTonKho).sum());
        return "branches/list";
    }

    @GetMapping("/branches/{maCH}")
    public ModelAndView detail(@PathVariable Integer maCH,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "moi-nhat") String sort) {
        try {
            BranchResponse branch = branchService.getPublic(maCH);
            BookSearchRequest request = new BookSearchRequest();
            request.setMaCH(maCH);
            request.setPage(Math.max(0, page));
            request.setSize(BOOK_PAGE_SIZE);
            String safeSort = java.util.Set.of("moi-nhat", "gia-tang", "gia-giam", "ten-az").contains(sort) ? sort : "moi-nhat";
            request.setSort(safeSort);
            ModelAndView result = new ModelAndView("branches/detail", "branch", branch);
            result.addObject("books", bookService.search(request));
            result.addObject("sort", safeSort);
            return result;
        } catch (BusinessException exception) {
            ModelAndView result = new ModelAndView("branches/not-found");
            result.setStatus(HttpStatus.NOT_FOUND);
            return result;
        }
    }
}
