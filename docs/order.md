# Đặt hàng

Luồng đặt hàng từ lúc thêm sách vào giỏ, tích chọn sản phẩm, chọn địa chỉ, vận chuyển và voucher, đến khi hệ thống tạo đơn. Hủy đơn xem [order-cancel.md](order-cancel.md); voucher xem [voucher.md](voucher.md); thanh toán chuyển khoản xem [payment.md](payment.md).

## 1. Tổng quan

```
Trang sách ──(Thêm vào giỏ)──► /cart ──(tích chọn + Đặt hàng)──► /checkout ──(Xác nhận)──┬─ COD ──────────► /order-success
                                  ▲                                  │                    └─ Chuyển khoản ─► /payment/pay ─(tiền về)─► /order-success
                                  └────── nếu chưa chọn gì ◄─────────┘
```

| Bước | Trang | Người dùng làm |
|---|---|---|
| 1 | Chi tiết hoặc danh sách sách | Bấm "Thêm vào giỏ" (hoặc dùng ngăn giỏ hàng nổi) |
| 2 | `/cart` | Tăng/giảm số lượng, xóa dòng, tích chọn sản phẩm muốn mua |
| 3 | `/checkout` | Chọn địa chỉ; với từng chi nhánh chọn đơn vị vận chuyển, ghi chú, voucher chi nhánh; chọn voucher trụ sở; chọn cách thanh toán |
| 4 | `/order-success` hoặc `/payment/pay` | Xem đơn vừa tạo, hoặc quét mã VietQR |
| 5 | `/orders` | Xem lại, theo dõi vận chuyển, hủy đơn, đánh giá |

Phải đăng nhập bằng tài khoản Khách hàng (`KHACH_HANG`).

## 2. Giỏ hàng

| Method | URL | Việc |
|---|---|---|
| GET | `/api/cart` | Giỏ hàng của tôi |
| POST | `/api/cart/items` | `{ "maSach": 12, "soLuong": 1 }`; sách đã có thì cộng dồn |
| PUT | `/api/cart/items/{maCTGioHang}` | Đổi số lượng |
| DELETE | `/api/cart/items/{maCTGioHang}` | Xóa một dòng |
| DELETE | `/api/cart` | Xóa cả giỏ |

`CartService` kiểm tra khi thêm/sửa:

- Sách hiện công khai (đã duyệt, đang bán, chi nhánh đã duyệt, tựa sách và danh mục đang bán).
- Sách có giá và còn hàng.
- Không mua sách của chính chi nhánh mình.
- Tổng số lượng trong giỏ không vượt tồn kho ("Bạn đã có N cuốn trong giỏ, cửa hàng chỉ còn M cuốn").

Mỗi dòng trả về có `maCTGioHang`, `maSach`, `tenSach`, `tacGia`, `hinhAnhUrl`, `maCH`, `tenCuaHang`, `donGia`, `soLuong`, `soLuongTon`, `thanhTien`, `canhBao`, `coTheMua`. `coTheMua = false` khi sách ngừng bán, hết hàng hoặc số lượng vượt tồn (lý do ở `canhBao`); dòng đó không tích chọn được.

Bấm "Đặt hàng": danh sách `maCTGioHang` đã tích được lưu vào `sessionStorage` khóa `ob_checkout_items` rồi chuyển sang `/checkout`. Lựa chọn chỉ tồn tại trong tab hiện tại.

## 3. Trang đặt hàng `/checkout`

Trang gọi:

| API | Để |
|---|---|
| `GET /api/profile/addresses` | Sổ địa chỉ; thêm/sửa/xóa ngay trên trang (`/api/locations/**` cho tỉnh, phường) |
| `GET /api/shipping-carriers` | Đơn vị vận chuyển đang hoạt động và phí (GHN 25.000đ, GHTK 20.000đ, VTP 22.000đ, JT 18.000đ mặc định) |
| `GET /api/payment/config` | Trụ sở đã bật chuyển khoản chưa |
| `POST /api/vouchers/platform/available`, `/platform/apply` | Voucher trụ sở dùng được, xem trước số giảm |
| `POST /api/vouchers/shop/available`, `/shop/apply` | Voucher chi nhánh dùng được, xem trước số giảm |

## 4. Yêu cầu tạo đơn

- COD: `POST /api/orders`
- Chuyển khoản: `POST /api/payment/sessions` (cùng body; xem [payment.md](payment.md))

```json
{
  "maDiaChi": 5,
  "phuongThucTT": "COD",
  "maCTGioHangs": [15, 18, 21],
  "codeVoucherSan": "TRUSO10",
  "storeOrders": [
    { "maCH": 2, "maDVVC": 1, "ghiChu": "Giao giờ hành chính", "codeVoucherShop": "CN2-GIAM20K" },
    { "maCH": 4, "maDVVC": 3, "ghiChu": null, "codeVoucherShop": null }
  ]
}
```

Mỗi chi nhánh có sách được chọn phải có **đúng một** phần tử trong `storeOrders`.

## 5. Backend xử lý (`OrderService.createOrder`)

1. Khóa giỏ hàng (chặn hai lần đặt đồng thời từ cùng một giỏ).
2. Lấy các dòng đã tích; khóa từng bản sách, kiểm tra còn bán và đủ tồn.
3. Gom sách theo chi nhánh → mỗi chi nhánh một đơn con; phí ship lấy theo đơn vị vận chuyển đã chọn.
4. Áp voucher chi nhánh cho từng đơn con, rồi voucher trụ sở cho cả đơn, phân bổ phần giảm của voucher trụ sở về các đơn con (xem [voucher.md](voucher.md)).
5. Lưu `don_hang` (đơn tổng), `don_hang_cua_hang` (đơn con, `CHO_XU_LY`), `chi_tiet_don_hang`; trừ tồn kho; ghi lượt dùng voucher.
6. Xóa khỏi giỏ **chỉ** các dòng đã đặt.
7. Sau khi commit: thông báo và đẩy WebSocket cho chi nhánh để trang đơn của chi nhánh cập nhật ngay.

Tính tiền:

```
tongTienHang   = Σ tiền hàng các đơn con
tongPhiShip    = Σ phí ship các đơn con
tongGiamGia    = giảm voucher trụ sở + Σ giảm voucher chi nhánh
tongThanhToan  = tongTienHang + tongPhiShip − tongGiamGia

Đơn con: tongTienShopOrder = tienHangCH + phiShipCH − giamGiaShop − giamGiaSanPhanBo
```

`POST /api/orders` chỉ nhận COD; gửi `CHUYEN_KHOAN` vào đây bị từ chối với thông báo dùng mã VietQR.

### Cấu trúc đơn hàng

```
don_hang (đơn tổng của khách: địa chỉ, phương thức TT, voucher trụ sở, tổng tiền)
 ├── don_hang_cua_hang (đơn con của chi nhánh A: đơn vị vận chuyển, voucher chi nhánh, trạng thái)
 │    └── chi_tiet_don_hang (từng bản sách)
 └── don_hang_cua_hang (đơn con của chi nhánh B)
```

### Vòng đời đơn con (`OrderStatus`)

```
CHO_XU_LY ──(chi nhánh xác nhận)──► DA_XAC_NHAN ──► DANG_LAY_HANG ──► DA_LAY_HANG ──► DANG_GIAO ──► DA_GIAO
    │                                    │                                              └──────────► DA_HOAN_TRA
    └──────────── DA_HUY ◄───────────────┘
```

Từ `DA_XAC_NHAN` trở đi, trạng thái do đơn vị vận chuyển cập nhật (xem [shipping.md](shipping.md)); chi nhánh không tự đổi. `DA_GIAO`, `DA_HOAN_TRA`, `DA_HUY` là trạng thái cuối.

Trạng thái thanh toán (`PaymentStatus`): `CHO_THANH_TOAN`, `DA_THANH_TOAN`, `THANH_TOAN_MOT_PHAN` (đơn tổng có đơn con đã giao và đơn con bị hoàn/hủy), `KHONG_THANH_TOAN`.

## 6. Sau khi đặt

| Method | URL | Việc |
|---|---|---|
| GET | `/api/orders` | Đơn của tôi |
| GET | `/api/orders/{maDH}` | Chi tiết, kèm hành trình vận chuyển từng đơn con |
| POST | `/api/orders/store-orders/{maDHCH}/cancel` | Hủy hoặc gửi yêu cầu hủy (xem [order-cancel.md](order-cancel.md)) |
| POST | `/api/reviews/order-items/{maCTDH}` | Đánh giá một sách đã nhận (JSON, hoặc multipart kèm ảnh/video) |

Phía chi nhánh (trang `/store/orders`):

| Method | URL | Việc |
|---|---|---|
| GET | `/api/store/orders?trangThai=` | Đơn của chi nhánh, lọc theo trạng thái, sản phẩm, đánh giá |
| GET | `/api/store/orders/products` | Danh sách sản phẩm để lọc |
| POST | `/api/store/orders/{maDHCH}/confirm` | Xác nhận → `DA_XAC_NHAN`, bàn giao cho đơn vị vận chuyển |
| POST | `/api/store/orders/{maDHCH}/cancel` | Hủy chủ động |
| POST | `/api/store/orders/{maDHCH}/cancel-request/approve` | Đồng ý yêu cầu hủy của khách |
| POST | `/api/store/orders/{maDHCH}/cancel-request/reject` | Từ chối yêu cầu hủy |
| POST | `/api/store/orders/{maDHCH}/sync-shipping` | Đồng bộ trạng thái vận chuyển ngay |

## 7. Thông báo lỗi thường gặp

| Thông báo | Nguyên nhân |
|---|---|
| Giỏ hàng đang trống | Không còn dòng nào trong giỏ |
| Sản phẩm được chọn không còn trong giỏ hàng... | Danh sách trong `sessionStorage` đã cũ (đặt ở tab khác) |
| Địa chỉ giao hàng không tồn tại hoặc không thuộc tài khoản của bạn | Sai `maDiaChi` |
| Số lượng vượt quá tồn kho... | Người khác vừa mua, hoặc chi nhánh vừa giảm tồn/điều chuyển sách đi |
| Sách hiện không còn được bán | Bản sách, chi nhánh, tựa sách hoặc danh mục vừa ngừng bán |
| Bạn không thể mua sách của chính cửa hàng mình | Chủ chi nhánh mua sách của chi nhánh mình |
| Đặt hàng trực tiếp chỉ hỗ trợ COD... | Gửi chuyển khoản vào `/api/orders` |

## 8. File liên quan

| File | Vai trò |
|---|---|
| `controller/cart/CartController.java`, `service/cart/CartService.java` | Giỏ hàng |
| `controller/order/OrderController.java`, `service/order/OrderService.java` | Đặt hàng, xem và hủy đơn |
| `controller/order/StoreOrderController.java`, `service/order/StoreOrderService.java` | Đơn của chi nhánh |
| `service/order/OrderSettlementService.java` | Quyết toán khi giao xong, hoàn hàng hoặc hủy |
| `service/order/OrderRealtimeService.java` | Đẩy thay đổi đơn qua WebSocket |
| `controller/order/ShippingCarrierController.java` | Đơn vị vận chuyển cho trang đặt hàng |
| `templates/cart.html`, `checkout.html`, `order-success.html`, `orders.html`, `store-orders.html` | Giao diện |
| `static/js/cart.js`, `cart-drawer.js`, `order-tracking.js`, `order-reviews.js` | Script trang |

## 9. Kiểm tra thủ công

Đăng nhập Khách hàng → thêm sách của hai chi nhánh vào giỏ → bỏ tích một dòng → đặt hàng COD, chọn đơn vị vận chuyển khác nhau cho hai chi nhánh → kiểm tra có hai đơn con, tồn kho giảm, dòng không tích vẫn còn trong giỏ → Chủ chi nhánh thấy đơn mới ngay (không tải lại) → xác nhận đơn → đơn vị vận chuyển cập nhật tới `DA_GIAO`.
