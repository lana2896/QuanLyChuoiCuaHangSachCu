package com.oldbook.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
// Nếu trường data bị null, Spring Boot sẽ tự động ẩn nó đi khỏi file JSON trả về
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    // Hàm tiện ích: Trả về thành công có dữ liệu
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .build();
    }

    // Hàm tiện ích: Trả về thành công nhưng không cần kèm dữ liệu (Ví dụ: Xóa thành công)
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .build();
    }

    // Hàm tiện ích: Trả về lỗi
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}