package com.oldbook.dto.store;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationSearchRequest {

    @Size(max = 255, message = "Từ khóa không được vượt quá 255 ký tự")
    private String tuKhoa;

    @NotNull(message = "Trạng thái duyệt không được để trống")
    @Pattern(regexp = "CHO_DUYET|DA_DUYET|TU_CHOI", message = "Trạng thái duyệt không hợp lệ")
    private String trangThaiDuyet = "CHO_DUYET";

    @Positive(message = "Mã cửa hàng phải lớn hơn 0")
    private Integer maCH;

    @NotNull(message = "Trang không được để trống")
    @Min(value = 0, message = "Trang không được âm")
    private Integer page = 0;

    @NotNull(message = "Kích thước trang không được để trống")
    @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100")
    @Max(value = 100, message = "Kích thước trang phải từ 1 đến 100")
    private Integer size = 20;
}
