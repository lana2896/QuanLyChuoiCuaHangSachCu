package com.oldbook.dto.identity;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeRoleRequest {

    @NotBlank(message = "Vai trò không được để trống")
    private String vaiTro;
}