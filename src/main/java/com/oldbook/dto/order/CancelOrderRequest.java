package com.oldbook.dto.order;

import com.oldbook.exception.common.BusinessException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @NotBlank(message = "Vui lòng nhập lý do")
        @Size(min = 50, max = 500, message = "Lý do phải từ 50 đến 500 ký tự")
        String lyDo
) {
    public static final int MIN_LENGTH = 50;

    public static String normalize(String lyDo) {
        String value = lyDo == null ? "" : lyDo.trim();
        if (value.length() < MIN_LENGTH) {
            throw new BusinessException("Lý do phải có ít nhất " + MIN_LENGTH + " ký tự");
        }
        return value;
    }
}
