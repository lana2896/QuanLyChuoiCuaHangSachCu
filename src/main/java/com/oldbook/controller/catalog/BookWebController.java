package com.oldbook.controller.catalog;

import com.oldbook.dto.catalog.BookSearchRequest;
import com.oldbook.service.catalog.BookService;
import com.oldbook.exception.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class BookWebController {

    private final BookService bookService;

    public BookWebController(BookService bookService) {
        this.bookService = bookService;
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
    public ModelAndView detailPage(@PathVariable Integer maSach) {
        try {
            return new ModelAndView("books/detail", "book", bookService.getDetail(maSach));
        } catch (BusinessException exception) {
            ModelAndView result = new ModelAndView("books/not-found");
            result.setStatus(HttpStatus.NOT_FOUND);
            return result;
        }
    }
}
