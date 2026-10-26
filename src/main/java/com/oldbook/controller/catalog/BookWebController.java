package com.oldbook.controller.catalog;

import com.oldbook.dto.catalog.BookSearchRequest;
import com.oldbook.service.branch.BranchService;
import com.oldbook.service.catalog.ReviewService;
import com.oldbook.service.catalog.BookService;
import com.oldbook.exception.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class BookWebController {

    private static final int REVIEW_PAGE_SIZE = 5;

    private final BookService bookService;
    private final ReviewService reviewService;
    private final BranchService branchService;

    public BookWebController(BookService bookService, ReviewService reviewService, BranchService branchService) {
        this.bookService = bookService;
        this.reviewService = reviewService;
        this.branchService = branchService;
    }

    @GetMapping({"/", "/books"})
    public String homePage(Model model) {
        return "books/catalog";
    }

    @GetMapping("/books/search")
    public String searchPage(@ModelAttribute("filters") BookSearchRequest request, Model model) {
        model.addAttribute("books", bookService.search(request));
        model.addAttribute("categories", bookService.getCategories());
        return "books/search";
    }

    @GetMapping("/books/{maSach}")
    public ModelAndView detailPage(@PathVariable Integer maSach,
                                   @RequestParam(defaultValue = "0") int page) {
        try {
            var book = bookService.getDetail(maSach);
            ModelAndView result = new ModelAndView("books/detail", "book", book);
            result.addObject("reviews", reviewService.getByBook(maSach, page, REVIEW_PAGE_SIZE));
            result.addObject("related", bookService.getRelated(maSach, book.maDM()));
            // Chuỗi cửa hàng: chi nhánh đang bán bản này và các chi nhánh khác còn cùng tựa sách.
            result.addObject("branch", branchService.getPublic(book.maCH()));
            result.addObject("otherBranches", bookService.getCopiesAtOtherBranches(maSach));
            return result;
        } catch (BusinessException exception) {
            ModelAndView result = new ModelAndView("books/not-found");
            result.setStatus(HttpStatus.NOT_FOUND);
            return result;
        }
    }
}
