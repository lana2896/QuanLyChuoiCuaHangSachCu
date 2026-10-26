package com.oldbook.dto.inventory;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Chi nhánh lập phiếu chuyển soLuong cuốn của bản sách maSach sang chi nhánh maCHDich. */
public record CreateStockTransferRequest(
        @NotNull(message = "Chọn sách cần điều chuyển") Integer maSach,
        @NotNull(message = "Chọn chi nhánh nhận") Integer maCHDich,
        @NotNull(message = "Nhập số lượng")
        @Min(value = 1, message = "Số lượng điều chuyển tối thiểu là 1")
        @Max(value = 10000, message = "Số lượng điều chuyển tối đa 10000") Integer soLuong,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String ghiChu
) {
}
