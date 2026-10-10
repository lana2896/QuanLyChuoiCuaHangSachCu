package com.oldbook.controller.catalog;

import com.oldbook.dto.catalog.BookResponse;
import com.oldbook.dto.catalog.PageResponse;
import com.oldbook.dto.catalog.BookSearchRequest;
import com.oldbook.dto.catalog.CategoryResponse;
import com.oldbook.service.catalog.BookService;
import com.oldbook.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category
    ) {
        return ApiResponse.success(bookService.searchPublicBooks(q, category));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<BookResponse>> search(@ModelAttribute BookSearchRequest request) {
        return ApiResponse.success(bookService.search(request));
    }

    @GetMapping("/{maSach}")
    public ApiResponse<BookResponse> getDetail(@PathVariable Integer maSach) {
        return ApiResponse.success(bookService.getDetail(maSach));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.success(bookService.getCategories());
    }
}
