# Vận chuyển: luồng giao hàng và cổng đối tác

Đơn vị vận chuyển dùng chung toàn chuỗi; khách chọn hãng cho từng đơn con lúc đặt hàng (xem [order.md](order.md)). Hãng **không đăng nhập**: ai có link `/shipping/{maCode}` thì vào được cổng của hãng đó.

## 1. Đơn vị vận chuyển

Khi khởi động, `ShippingDataInitializer` tạo sẵn (nếu chưa có theo `ma_code`):

| `maCode` | Tên | Phí mặc định |
|---|---|---|
| `GHN` | Giao Hàng Nhanh | 25.000đ |
| `GHTK` | Giao Hàng Tiết Kiệm | 20.000đ |
| `VTP` | Viettel Post | 22.000đ |
| `JT` | J&T Express | 18.000đ |

`maCode` chỉ gồm `A-Z`, `0-9`, `-`, `_` (2–30 ký tự), không phân biệt hoa thường (`/shipping/ghn` và `/shipping/GHN` là một). Hãng `NGUNG_HOAT_DONG` (`CarrierStatus`) không hiện lúc đặt hàng và không tiếp nhận/cập nhật đơn được.

Quản lý thêm/sửa hãng ở `/management/shipping-carriers`:

| Method | URL | Việc |
|---|---|---|
| GET | `/api/management/shipping-carriers` | Danh sách kèm thống kê |
| GET | `/api/management/shipping-carriers/{maCode}` | Chi tiết |
| POST | `/api/management/shipping-carriers` | Thêm: `maCode`, `tenDonVi`, `phiVanChuyen`, người liên hệ, SĐT, email, địa chỉ, website, ghi chú, thông tin tích hợp |
| PUT | `/api/management/shipping-carriers/{maCode}` | Sửa |
| PATCH | `/api/management/shipping-carriers/{maCode}/status` | `HOAT_DONG` / `NGUNG_HOAT_DONG` |

Khách lấy danh sách hãng đang hoạt động qua `GET /api/shipping-carriers`.

## 2. Một luồng trạng thái duy nhất

Sau khi chi nhánh xác nhận (`POST /api/store/orders/{maDHCH}/confirm`), đơn được bàn giao cho hãng: tạo `van_chuyen` ở `CHO_LAY_HANG`, sinh mã vận đơn, ghi một dòng `lich_su_van_chuyen`.

```
CHO_LAY_HANG → DANG_LAY_HANG → DA_LAY_HANG → DANG_GIAO → DA_GIAO
      └────────────┴──────────────┴──────────►    │
                                                  └──► GIAO_THAT_BAI → DANG_GIAO (giao lại)
                                                                   └─(hết lượt, hệ thống tự chuyển)─► DA_HOAN_TRA
```

Bước hãng được phép báo (`ShipmentStatus.canMoveTo`):

| Từ | Sang |
|---|---|
| `CHO_LAY_HANG` | `DANG_LAY_HANG`, `DA_LAY_HANG`, `DANG_GIAO` |
| `DANG_LAY_HANG` | `DA_LAY_HANG`, `DANG_GIAO` |
| `DA_LAY_HANG` | `DANG_GIAO` |
| `DANG_GIAO` | `DA_GIAO`, `GIAO_THAT_BAI` |
| `GIAO_THAT_BAI` | `DANG_GIAO` |

- Hãng **không** tự chọn `DA_HOAN_TRA`: khi số lần giao thất bại đạt `shipping.max-delivery-attempts` (mặc định 3), hệ thống tự chuyển hoàn hàng.
- `DA_HUY` chỉ xảy ra khi chi nhánh hủy đơn trước lúc hãng tiếp nhận (xem [order-cancel.md](order-cancel.md)).
- Mỗi trạng thái vận chuyển kéo theo trạng thái đơn con (`ShipmentStatus.toOrderStatus`); `GIAO_THAT_BAI` vẫn để đơn con ở `DANG_GIAO`. Chi nhánh **không** tự đổi trạng thái giao hàng.

Mọi thay đổi đi qua `ShipmentSyncService`:

| Nguồn | Hàm | `lich_su_van_chuyen.nguon` |
|---|---|---|
| Tra cứu API của hãng (định kỳ hoặc nút đồng bộ của chi nhánh) | `applyCarrierUpdate` | `POLLING` |
| Hãng thao tác trên cổng `/shipping/{maCode}` | `applyPartnerUpdate` | `DOI_TAC` |
| Hệ thống tự chuyển (xác nhận, hết lượt giao → hoàn hàng) | — | `HE_THONG` |

Cả hai đường dùng chung: kiểm tra bước chuyển, đếm số lần giao thất bại, tự hoàn hàng khi hết lượt, **quyết toán** khi `DA_GIAO` (chiết khấu, doanh thu chi nhánh, lập khoản chi cho chi nhánh, ghi sổ thu COD) hoặc hoàn kho khi `DA_HOAN_TRA` (đơn chuyển khoản thì lập khoản hoàn tiền). Xem `OrderSettlementService`.

## 3. Chọn nguồn cập nhật: `shipping.sync.provider`

| Giá trị | Mã vận đơn | Trạng thái đổi khi |
|---|---|---|
| `portal` (đang dùng trong `application.properties`) | hệ thống sinh | hãng cập nhật trên cổng đối tác |
| `simulator` | hệ thống sinh | mỗi chu kỳ đồng bộ tự tiến 1 bước (demo) |
| `rest` | API hãng cấp | tra cứu API hãng theo `shipping.carriers.<MA>.*` |

- Không dùng `simulator` cùng cổng đối tác: giả lập tự đẩy trạng thái, tranh với thao tác của hãng.
- Đồng bộ định kỳ bật bằng `shipping.sync.polling-enabled=true`, chu kỳ `shipping.sync.polling-interval-ms` (mặc định 60 giây). Chi nhánh đồng bộ ngay một đơn bằng `POST /api/store/orders/{maDHCH}/sync-shipping`.
- Cấu hình `rest` cho từng hãng (`ShippingProperties.Carrier`): URL tạo đơn/tra cứu (có thể dùng `{maVanDon}`), header chứa token, JSON pointer tới mã vận đơn và trạng thái trong phản hồi, bảng ánh xạ trạng thái riêng.

## 4. Cổng đối tác

Trang `/shipping/{maCode}` (danh sách hãng ở `/shipping`). Trang nhận đơn mới và thay đổi (ví dụ chi nhánh hủy) qua WebSocket `/ws/shipping`.

API (không cần đăng nhập, giới hạn theo `{maCode}`):

| Method | URL | Ghi chú |
|---|---|---|
| GET | `/api/shipping/{maCode}` | Thông tin hãng |
| GET | `/api/shipping/{maCode}/requests/pending` | Đơn `CHO_LAY_HANG` chờ tiếp nhận |
| POST | `/api/shipping/{maCode}/requests` | `{ "maDHCH": 10, "ghiChuGiaoHang": "..." }` → `DANG_LAY_HANG` |
| GET | `/api/shipping/{maCode}/requests` | Đơn của hãng, kèm `trangThaiTiepTheo` (bước được phép) |
| GET | `/api/shipping/{maCode}/requests/{id}` | Chi tiết, lịch sử |
| PATCH | `/api/shipping/{maCode}/requests/{id}/status` | `{ "trangThai": "DANG_GIAO", "ghiChu": "..." }` |
| PATCH | `/api/shipping/{maCode}/requests/{id}/result` | Như trên, chỉ nhận `DA_GIAO` / `GIAO_THAT_BAI` |

Mã vận đơn cố định từ lúc chi nhánh xác nhận (dùng để tra cứu và đối soát COD); cổng không đổi được. Đơn chuyển khoản có số tiền thu hộ bằng 0.

## 5. File liên quan

| File | Vai trò |
|---|---|
| `constant/shipping/ShipmentStatus.java` | Trạng thái, bước được phép, ánh xạ sang trạng thái đơn |
| `config/shipping/ShippingProperties.java`, `ShippingDataInitializer.java`, `ShipmentSocketConfig.java` | Cấu hình, dữ liệu hãng mặc định, WebSocket `/ws/shipping` |
| `controller/shipping/ShipmentController.java`, `ShippingWebController.java` | API và trang cổng đối tác |
| `controller/shipping/CarrierManagementController.java` | Quản lý hãng |
| `service/shipping/ShipmentSyncService.java` | Điểm duy nhất đổi trạng thái vận chuyển |
| `service/shipping/ShippingCarrierResolver.java` | Xác định hãng từ `maCode` trên URL |
| `service/shipping/PortalShippingCarrierClient.java`, `SimulatedShippingCarrierClient.java`, `RestShippingCarrierClient.java` | Ba nguồn cập nhật |
| `service/shipping/ShipmentPollingScheduler.java` | Đồng bộ định kỳ |
| `templates/shipping/`, `static/js/shipping/dashboard.js` | Giao diện cổng và quản lý hãng |

## 6. Kiểm tra thủ công

Đặt đơn chọn GHN → chi nhánh xác nhận → mở `/shipping/GHN`, thấy đơn ở "chờ tiếp nhận" → tiếp nhận → cập nhật `DA_LAY_HANG` → `DANG_GIAO` → `GIAO_THAT_BAI` → `DANG_GIAO` → ... đến lần thất bại thứ 3: đơn tự thành `DA_HOAN_TRA`, tồn kho trở lại → đơn khác cập nhật `DA_GIAO`: Quản lý thấy khoản chi cho chi nhánh, khách đánh giá được sách.
