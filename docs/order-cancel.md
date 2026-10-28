# Hủy đơn hàng

Hủy luôn tính trên **đơn con** (`don_hang_cua_hang`) của một chi nhánh, không hủy cả đơn tổng một lần. Đặt hàng xem [order.md](order.md); hoàn tiền đơn chuyển khoản xem [payment.md](payment.md).

## 1. Ai được hủy khi nào

| Trạng thái đơn con | Vận chuyển | Khách hàng | Chủ chi nhánh |
|---|---|---|---|
| `CHO_XU_LY` | chưa có | **Hủy ngay** | **Hủy ngay** |
| `DA_XAC_NHAN` | `CHO_LAY_HANG` (hãng chưa tiếp nhận) | **Gửi yêu cầu hủy** | Hủy ngay; hoặc duyệt yêu cầu của khách |
| `DA_XAC_NHAN` / `DANG_LAY_HANG` | `DANG_LAY_HANG` | — | Chỉ **duyệt yêu cầu** đã gửi trước đó |
| Từ `DA_LAY_HANG` trở đi | hãng đã lấy hàng | Không hủy được | Không hủy được; phải từ chối yêu cầu |

- Mọi thao tác hủy và từ chối yêu cầu hủy đều bắt buộc lý do **từ 50 đến 500 ký tự** (`CancelOrderRequest`).
- Khách chỉ được gửi **một** yêu cầu hủy cho mỗi đơn con ("Bạn đã gửi yêu cầu hủy cho đơn hàng này").
- Chi nhánh hủy chủ động khi còn yêu cầu đang chờ thì yêu cầu đó được tự đánh dấu `DA_DUYET`.
- Trạng thái yêu cầu (`CancellationStatus`, bảng `yeu_cau_huy_don`): `CHO_DUYET`, `DA_DUYET`, `TU_CHOI`.
- `huyBoi` của đơn con ghi `KHACH_HANG` hoặc `CHU_CUA_HANG`, kèm `lyDoHuy`, `ngayHuy`.

## 2. API

| Method | URL | Ai | Việc |
|---|---|---|---|
| POST | `/api/orders/store-orders/{maDHCH}/cancel` | Khách | `{ "lyDo": "..." }`: hủy ngay hoặc gửi yêu cầu, tùy trạng thái |
| POST | `/api/store/orders/{maDHCH}/cancel` | Chi nhánh | Hủy chủ động |
| POST | `/api/store/orders/{maDHCH}/cancel-request/approve` | Chi nhánh | Đồng ý yêu cầu hủy |
| POST | `/api/store/orders/{maDHCH}/cancel-request/reject` | Chi nhánh | Từ chối, `{ "lyDo": "..." }` |
| GET / PUT | `/api/payment/orders/{maDH}/refund-account` | Khách | Xem / chọn tài khoản nhận hoàn tiền trước khi hủy đơn chuyển khoản |

## 3. Sơ đồ

```
Khách bấm Hủy
   ├─ CHO_XU_LY ─────────────────────────────► DA_HUY (huyBoi = KHACH_HANG)
   ├─ DA_XAC_NHAN ──► yêu cầu CHO_DUYET ──┬─ chi nhánh đồng ý (hãng chưa lấy hàng) ─► DA_HUY
   │                                      └─ chi nhánh từ chối ─► đơn tiếp tục giao
   └─ đã lấy hàng ──► báo lỗi "Đơn hàng đã được đơn vị vận chuyển tiếp nhận, không thể hủy"

Chi nhánh bấm Hủy
   ├─ CHO_XU_LY ─────────────────────────────► DA_HUY (huyBoi = CHU_CUA_HANG)
   ├─ DA_XAC_NHAN + vận chuyển CHO_LAY_HANG ─► DA_HUY, vận đơn → DA_HUY, báo cho đơn vị vận chuyển
   └─ còn lại ──► "Đơn vị vận chuyển đã tiếp nhận đơn, không thể hủy"
```

## 4. Ảnh hưởng khi hủy (`OrderSettlementService.settleCancelled`)

| Mục | Thay đổi |
|---|---|
| Tồn kho | Cộng lại đủ số lượng của từng bản sách trong đơn con |
| Vận đơn | Nếu đã có thì chuyển `DA_HUY`, đơn vị vận chuyển nhận cập nhật qua WebSocket |
| Chiết khấu, doanh thu chi nhánh | Về 0 |
| Thanh toán đơn con | `KHONG_THANH_TOAN` |
| Tiền hoàn (`tienHoanTra`) | COD: 0 (chưa thu tiền). Chuyển khoản: cả tiền hàng lẫn phí vận chuyển của đơn con |
| Khoản hoàn | Đơn chuyển khoản: lập yêu cầu `HOAN_TIEN_KHACH` (xem [payment.md](payment.md)) |
| Voucher chi nhánh | Trả lại lượt dùng |
| Voucher trụ sở | Chỉ trả lại lượt khi **mọi** đơn con của đơn tổng đều bị hủy |
| Đơn tổng | Tính lại trạng thái thanh toán từ các đơn con |

Hủy lần hai không làm gì thêm (đơn đã `DA_HUY` thì bỏ qua), nên không cộng kho hai lần.

### So sánh với hoàn hàng (`settleReturned`)

Khi giao thất bại quá số lần cho phép, đơn con thành `DA_HOAN_TRA` (xem [shipping.md](shipping.md)): tồn kho cũng được cộng lại, nhưng với đơn chuyển khoản **phí vận chuyển không được hoàn** (`tienHoanTra = tổng đơn con − phí ship`), vì hãng đã đi giao.

## 5. Thông báo

Mỗi bước gửi thông báo cho bên kia (`BusinessNotificationService`): khách hủy, khách gửi yêu cầu, chi nhánh hủy, chi nhánh đồng ý/từ chối yêu cầu. Trang đơn của khách và chi nhánh tự cập nhật qua WebSocket (`OrderRealtimeService`).

## 6. Thông báo lỗi thường gặp

| Thông báo | Nguyên nhân |
|---|---|
| Đơn hàng không tồn tại | Đơn không thuộc khách / chi nhánh đang đăng nhập |
| Bạn đã gửi yêu cầu hủy cho đơn hàng này | Đã có yêu cầu (kể cả đã bị từ chối) |
| Đơn hàng đã được đơn vị vận chuyển tiếp nhận, không thể hủy | Khách hủy khi đơn đã qua `DA_XAC_NHAN` |
| Đơn vị vận chuyển đã tiếp nhận đơn, không thể hủy | Chi nhánh hủy khi hãng đã tiếp nhận |
| Đơn vị vận chuyển đã lấy hàng, không thể hủy. Vui lòng từ chối yêu cầu hủy | Duyệt yêu cầu khi hãng đã lấy hàng |
| Chỉ đơn chuyển khoản mới có khoản hoàn tiền | Chọn tài khoản hoàn cho đơn COD |

## 7. File liên quan

| File | Vai trò |
|---|---|
| `service/order/OrderService.java` (`cancelStoreOrder`) | Khách hủy / gửi yêu cầu |
| `service/order/StoreOrderService.java` | Chi nhánh hủy, duyệt/từ chối yêu cầu |
| `service/order/OrderSettlementService.java` | Hoàn kho, tiền hoàn, voucher, trạng thái thanh toán |
| `service/payment/OrderRefundAccountService.java` | Tài khoản nhận hoàn tiền khách chọn lúc hủy |
| `entity/order/OrderCancellation.java` | Bảng `yeu_cau_huy_don` |

## 8. Kiểm tra thủ công

1. Đặt đơn COD → hủy ở `CHO_XU_LY` → tồn kho trở lại, voucher trả lượt.
2. Đặt đơn → chi nhánh xác nhận → khách gửi yêu cầu → chi nhánh từ chối → khách không gửi lại được.
3. Đặt đơn chuyển khoản → chi nhánh xác nhận → khách chọn tài khoản hoàn và gửi yêu cầu → chi nhánh đồng ý → Quản lý thấy khoản hoàn ở `/management/payments?tab=refunds`.
4. Hai đơn con dùng voucher trụ sở → hủy một đơn con: voucher chưa trả lượt; hủy nốt đơn còn lại: voucher trả lượt.
