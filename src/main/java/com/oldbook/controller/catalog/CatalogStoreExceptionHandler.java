package com.oldbook.controller.catalog;

import com.oldbook.dto.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = {
        "com.oldbook.controller.catalog",
        "com.oldbook.controller.store"
})
public class CatalogStoreExceptionHandler {

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBinding(BindException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .map(error -> error instanceof FieldError field && field.isBindingFailure()
                        ? "Tham số " + field.getField() + " không đúng định dạng"
                        : error.getDefaultMessage())
                .findFirst().orElse("Dữ liệu không hợp lệ");
        return badRequest(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return badRequest("Tham số " + exception.getName() + " không đúng định dạng");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return badRequest("Nội dung JSON không hợp lệ hoặc có trường sai kiểu dữ liệu");
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ApiResponse<Void>> handleParameterValidation(Exception exception) {
        return badRequest("Tham số không hợp lệ; kiểm tra mã, bộ lọc và phân trang");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrity(DataIntegrityViolationException exception) {
        return badRequest("Dữ liệu bị trùng hoặc không còn hợp lệ; vui lòng kiểm tra và thử lại");
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }
}
