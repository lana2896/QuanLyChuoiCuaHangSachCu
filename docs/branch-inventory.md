# Chi nhánh, tồn kho toàn chuỗi và điều chuyển sách

Phần riêng của mô hình **chuỗi cửa hàng**: khách xem hệ thống chi nhánh, trụ sở xem tồn kho của mọi chi nhánh, và các chi nhánh chuyển sách cho nhau. Danh mục, tựa sách, nhập sách và kiểm duyệt xem [catalog-store.md](catalog-store.md).

## 1. Hệ thống chi nhánh (công khai)

| Trang | Nội dung |
|---|---|
| `/branches?tinh=&q=` | Các chi nhánh đã được duyệt, sắp theo số chi nhánh; lọc theo tỉnh/thành và từ khóa (tên, địa chỉ, không phân biệt dấu) |
| `/branches/{maCH}` | Thông tin một chi nhánh và sách đang bán ở đó |

| Method | URL | Việc |
|---|---|---|
| GET | `/api/branches?maTinh&q` | Danh sách chi nhánh |
| GET | `/api/branches/provinces` | Các tỉnh/thành có chi nhánh, kèm số chi nhánh mỗi tỉnh |
| GET | `/api/branches/{maCH}` | Một chi nhánh; chưa duyệt thì báo "Chi nhánh không tồn tại hoặc chưa hoạt động" |

Mỗi chi nhánh trả kèm `soDauSach` (số bản sách đang bán công khai) và `tongTonKho` (tổng số cuốn tồn của các bản đó). Chỉ tính sách đã duyệt và đang bán.

Trang chi tiết sách cũng cho biết cùng tựa sách đang có ở những chi nhánh nào (xem [catalog-store.md](catalog-store.md#7-tra-cứu-công-khai)).

## 2. Tồn kho toàn chuỗi (Quản lý)

Trang `/management/inventory`, dùng để biết chi nhánh nào thừa, chi nhánh nào thiếu rồi điều phối điều chuyển.

| Method | URL | Việc |
|---|---|---|
| GET | `/api/management/inventory?tuKhoa=` | Tìm tựa sách (tối đa 50 tựa, 500 bản sách); mỗi tựa có `tongTon`, `soChiNhanh` và danh sách `chiNhanh` (mã sách, độ mới, giá, tồn, trạng thái bán) |
| GET | `/api/management/transfers?trangThai=` | Các phiếu điều chuyển toàn chuỗi (tối đa 200 phiếu mới nhất), lọc theo trạng thái |

Quản lý chỉ xem; việc lập phiếu do chi nhánh gửi thực hiện.

## 3. Điều chuyển sách giữa chi nhánh

### 3.1. Trạng thái phiếu (`StockTransferStatus`)

```
            ┌──(chi nhánh nhận xác nhận)──► DA_NHAN
CHO_NHAN ───┼──(chi nhánh nhận từ chối)───► TU_CHOI   (trả sách về kho gửi)
            └──(chi nhánh gửi hủy)────────► DA_HUY    (trả sách về kho gửi)
```

| Trạng thái | Ý nghĩa | Tồn kho |
|---|---|---|
| `CHO_NHAN` | Đang chuyển, chờ chi nhánh nhận | Đã trừ khỏi bản sách gửi ngay khi lập phiếu |
| `DA_NHAN` | Đã nhập kho chi nhánh nhận | Cộng vào bản sách ở chi nhánh nhận |
| `TU_CHOI` | Chi nhánh nhận từ chối (bắt buộc lý do) | Cộng lại bản sách gửi |
| `DA_HUY` | Chi nhánh gửi hủy khi phiếu còn `CHO_NHAN` | Cộng lại bản sách gửi |

### 3.2. Khi chi nhánh nhận xác nhận

Hệ thống tìm ở chi nhánh nhận một bản sách **cùng tựa, cùng độ mới, cùng mô tả tình trạng**:

- có thì cộng dồn tồn kho vào bản đó;
- chưa có thì tạo bản sách mới, sao chép ảnh, trạng thái **`DA_DUYET` sẵn** (sách đã qua kiểm duyệt ở chi nhánh gửi).

Bản sách đích được ghi vào `phieu_dieu_chuyen.sachDich`.

### 3.3. Điều kiện lập phiếu

- Chi nhánh gửi đã được duyệt; bản sách thuộc chi nhánh gửi và đã được duyệt.
- Chi nhánh nhận là một chi nhánh **khác**, đang hoạt động.
- Số lượng ≥ 1 và không vượt tồn kho hiện tại ("Kho chi nhánh chỉ còn N cuốn của bản sách này").
- Chỉ phiếu `CHO_NHAN` mới được xử lý; xử lý lần hai báo "Phiếu điều chuyển này đã được xử lý".

Thứ tự khóa giống quản lý sách: khóa chi nhánh của người thao tác, rồi phiếu, rồi bản sách, nên hai thao tác đồng thời không trừ/cộng kho sai.

### 3.4. API của Chủ chi nhánh

Trang `/store/transfers`.

| Method | URL | Việc |
|---|---|---|
| GET | `/api/store/transfers` | Phiếu gửi đi và nhận về của chi nhánh tôi |
| GET | `/api/store/transfers/destinations` | Các chi nhánh khác đang hoạt động để chọn nơi nhận |
| POST | `/api/store/transfers` | Lập phiếu: `{ "maSach": 15, "maCHDich": 3, "soLuong": 2, "ghiChu": "..." }` |
| POST | `/api/store/transfers/{maPhieu}/accept` | Chi nhánh nhận xác nhận |
| POST | `/api/store/transfers/{maPhieu}/reject` | Chi nhánh nhận từ chối: `{ "lyDo": "..." }` |
| POST | `/api/store/transfers/{maPhieu}/cancel` | Chi nhánh gửi hủy |

Mỗi bước gửi thông báo cho chi nhánh bên kia (`BusinessNotificationService.stockTransferIncoming/Accepted/Rejected/Cancelled`).

## 4. File liên quan

| File | Vai trò |
|---|---|
| `controller/branch/BranchController.java`, `BranchWebController.java` | API và trang hệ thống chi nhánh |
| `service/branch/BranchService.java` | Danh sách chi nhánh, đếm sách theo chi nhánh |
| `controller/inventory/StoreTransferController.java` | API điều chuyển của chi nhánh |
| `controller/inventory/ManagementInventoryController.java` | API tồn kho chuỗi và phiếu điều chuyển cho Quản lý |
| `service/inventory/StockTransferService.java` | Vòng đời phiếu điều chuyển |
| `service/inventory/ChainInventoryService.java` | Tra tồn kho theo tựa sách |
| `entity/inventory/StockTransfer.java` | Bảng `phieu_dieu_chuyen` |
| `templates/branches/`, `store-transfers.html`, `management/inventory.html` | Giao diện |
| `static/js/store-transfers.js`, `static/js/management/inventory.js` | Script trang |

## 5. Kiểm tra thủ công

Tạo hai chi nhánh A, B đã duyệt → A có bản sách tồn 5 → A lập phiếu chuyển 2 cuốn sang B: tồn của A còn 3 → B thấy phiếu và thông báo → B xác nhận: B có bản sách mới (đã duyệt) tồn 2, hiện công khai → lập phiếu khác rồi B từ chối (hoặc A hủy): tồn của A trở lại như trước → Quản lý tra tựa sách ở `/management/inventory` thấy đúng tồn từng chi nhánh.
