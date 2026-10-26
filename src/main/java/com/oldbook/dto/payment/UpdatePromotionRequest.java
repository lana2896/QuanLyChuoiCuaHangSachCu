package com.oldbook.dto.payment;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class UpdatePromotionRequest {

    @NotBlank(message = "Tên chương trình không được để trống")
    @Size(max = 255, message = "Tên chương trình tối đa 255 ký tự")
    private String tenKhuyenMai;

    @NotBlank(message = "Vui lòng chọn loại giảm giá")
    @Pattern(regexp = "^(THEO_PHAN_TRAM|CO_DINH)$", message = "Loại giảm giá không hợp lệ")
    private String loaiGiamGia;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    @DecimalMax(value = "1000000000", message = "Giá trị giảm quá lớn")
    private BigDecimal giaTriGiam;

    @DecimalMin(value = "0", message = "Mức giảm tối đa không được âm")
    @DecimalMax(value = "1000000000", message = "Mức giảm tối đa quá lớn")
    private BigDecimal giaTriGiamToiDa;

    @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không được âm")
    @DecimalMax(value = "1000000000", message = "Giá trị đơn tối thiểu quá lớn")
    private BigDecimal giaTriDonToiThieu;

    @NotNull(message = "Số lượng phát hành không được để trống")
    @Min(value = 1, message = "Số lượng phát hành phải lớn hơn 0")
    @Max(value = 1000000, message = "Số lượng phát hành tối đa 1.000.000")
    private Integer soLuong;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime ngayBatDau;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime ngayKetThuc;
}
