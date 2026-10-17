package com.oldbook.dto.shipping;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeCarrierStatusRequest {
    /** HOAT_DONG hoặc NGUNG_HOAT_DONG */
    @NotBlank
    private String trangThai;
}
