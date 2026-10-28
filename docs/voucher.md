# Voucher

Chuỗi có hai loại voucher (bảng `khuyen_mai`, `VoucherType`):

| Loại | Mã | Ai tạo | Áp cho | `cuaHang` |
|---|---|---|---|---|
| Voucher trụ sở (toàn chuỗi) | `VOUCHER_SAN` | Quản lý, trang `/management/vouchers` | Cả đơn tổng | `null` |
| Voucher chi nhánh | `VOUCHER_SHOP` | Chủ chi nhánh, trang `/store/vouchers` | Đơn con của đúng chi nhánh đó | chi nhánh tạo |

Một đơn dùng tối đa một voucher trụ sở, và mỗi đơn con tối đa một voucher của chi nhánh mình. Đặt hàng xem [order.md](order.md).

## 1. Thuộc tính voucher

| Trường | Ý nghĩa |
|---|---|
| `code` | 3–50 ký tự chữ, số, `-`, `_`; lưu chữ hoa; **duy nhất trên toàn hệ thống** (không trùng giữa trụ sở và mọi chi nhánh) |
| `tenKhuyenMai` | Tên hiển thị |
| `loaiGiamGia` | `THEO_PHAN_TRAM` (≤ 100) hoặc `CO_DINH` (số tiền) |
| `giaTriGiam` | Phần trăm hoặc số tiền giảm |
| `giaTriGiamToiDa` | Mức giảm tối đa (dùng cho loại phần trăm) |
| `giaTriDonToiThieu` | Tiền hàng tối thiểu để áp dụng |
| `soLuong`, `daSuDung` | Số lượt phát hành, số lượt đã dùng |
| `ngayBatDau`, `ngayKetThuc` | Thời gian hiệu lực; kết thúc phải sau bắt đầu |
| `trangThai` | `HOAT_DONG` hoặc `TAM_DUNG` (`PromotionStatus`) |

Quy tắc sửa/xóa:

- Không giảm `soLuong` xuống dưới số lượt đã dùng.
- Voucher đã dùng trong đơn hàng thì **không xóa được**, chỉ tạm dừng.
- Chi nhánh chỉ thấy và sửa voucher của chính mình.

## 2. Điều kiện dùng được

Voucher hợp lệ khi: đang `HOAT_DONG`, thời điểm hiện tại nằm trong `[ngayBatDau, ngayKetThuc]`, còn lượt, và tiền hàng đạt `giaTriDonToiThieu`. Voucher chi nhánh còn phải thuộc đúng chi nhánh của đơn con.

| Thông báo | Nguyên nhân |
|---|---|
| Mã voucher không tồn tại hoặc không hợp lệ | Sai mã hoặc sai loại |
| Voucher không còn hiệu lực hoặc không hợp lệ | Tạm dừng hoặc ngoài thời gian |
| Voucher đã hết lượt sử dụng | `daSuDung ≥ soLuong` |
| Đơn hàng chưa đạt giá trị tối thiểu ... để áp dụng voucher | Chưa đủ tiền hàng |
| Voucher không áp dụng cho cửa hàng này | Voucher của chi nhánh khác |

## 3. Cách tính

```
giảm = THEO_PHAN_TRAM ? tiền hàng × giaTriGiam / 100 : giaTriGiam      (làm tròn xuống đồng)
giảm = min(giảm, giaTriGiamToiDa nếu có, tiền hàng)
```

Thứ tự khi đặt hàng (`OrderService`):

1. **Voucher chi nhánh** tính trên tiền hàng của đơn con → `giamGiaShop`.
2. **Voucher trụ sở** tính trên tổng tiền hàng, nhưng không vượt (tổng tiền hàng − tổng giảm của voucher chi nhánh).
3. Phần giảm của voucher trụ sở được **phân bổ** về các đơn con theo tỷ lệ tiền hàng, mỗi đơn con không nhận quá phần tiền hàng còn lại sau voucher chi nhánh; phần lẻ dồn cho đơn con còn chỗ → `giamGiaSanPhanBo` (`VoucherService.allocate`).

Phí vận chuyển không được giảm.

## 4. Ai chịu tiền giảm

Khi đơn con giao thành công (`OrderSettlementService.settleDelivered`):

```
cơ sở tính chiết khấu mỗi dòng = thành tiền − phần voucher chi nhánh chia theo tỷ lệ
chiết khấu (trụ sở thu)        = Σ cơ sở × tỷ lệ chiết khấu của danh mục
doanh thu chi nhánh            = tienHangCH − giamGiaShop − chiết khấu
```

- **Voucher chi nhánh**: chi nhánh chịu (trừ vào doanh thu chi nhánh, và không tính chiết khấu trên phần đã giảm).
- **Voucher trụ sở**: trụ sở chịu; doanh thu chi nhánh không bị trừ.

## 5. Trả lượt khi hủy

- Hủy một đơn con → trả lượt voucher chi nhánh của đơn con đó.
- Voucher trụ sở chỉ trả lượt khi **mọi** đơn con của đơn tổng đều bị hủy.

Lượt dùng ghi ở bảng `su_dung_voucher` (`VoucherUsage`).

## 6. API

### Khách hàng (trang đặt hàng)

Body chung: `{ "maCTGioHangs": [15, 18], "code": "TRUSO10", "maCH": 2 }` (`maCH` chỉ cho voucher chi nhánh).

| Method | URL | Việc |
|---|---|---|
| POST | `/api/vouchers/platform/available` | Voucher trụ sở dùng được cho các dòng đã chọn |
| POST | `/api/vouchers/platform/apply` | Xem trước số giảm của một mã |
| POST | `/api/vouchers/shop/available` | Voucher chi nhánh dùng được, theo từng chi nhánh trong giỏ |
| POST | `/api/vouchers/shop/apply` | Xem trước số giảm của một mã chi nhánh |

"Apply" chỉ xem trước, không giữ lượt. Lượt chỉ bị trừ khi đơn được tạo (khóa dòng voucher trong giao dịch tạo đơn).

### Quản lý — voucher trụ sở

| Method | URL | Việc |
|---|---|---|
| GET | `/api/management/vouchers?tuKhoa&trangThai` | Danh sách kèm thống kê |
| GET | `/api/management/vouchers/{maKM}` | Chi tiết |
| POST | `/api/management/vouchers` | Tạo |
| PUT | `/api/management/vouchers/{maKM}` | Sửa (không đổi `code`) |
| PATCH | `/api/management/vouchers/{maKM}/status` | `HOAT_DONG` / `TAM_DUNG` |
| DELETE | `/api/management/vouchers/{maKM}` | Xóa nếu chưa dùng |

### Chủ chi nhánh — voucher chi nhánh

Cùng cấu trúc dưới `/api/store/vouchers` (`StorePromotionController`), phạm vi là chi nhánh của người đăng nhập.

## 7. File liên quan

| File | Vai trò |
|---|---|
| `controller/payment/VoucherController.java` | API cho khách |
| `controller/payment/PromotionManagementController.java`, `StorePromotionController.java` | API quản lý voucher trụ sở / chi nhánh |
| `controller/payment/VoucherWebController.java` | Trang `/management/vouchers`, `/store/vouchers` |
| `service/payment/VoucherService.java` | Kiểm tra, tính, phân bổ, ghi và trả lượt |
| `service/payment/PromotionManagementService.java` | Tạo, sửa, đổi trạng thái, xóa |
| `templates/management/vouchers.html`, `store-vouchers.html` | Giao diện |

## 8. Kiểm tra thủ công

Quản lý tạo voucher trụ sở 10% tối đa 30.000đ, đơn tối thiểu 100.000đ → chi nhánh A tạo voucher giảm cố định 20.000đ → khách đặt sách của A và B, áp cả hai → kiểm tra `giamGiaShop` ở đơn con A, `giamGiaSanPhanBo` chia theo tỷ lệ tiền hàng → giao thành công đơn A, doanh thu A = tiền hàng − 20.000 − chiết khấu → thử xóa voucher đã dùng: bị từ chối.
