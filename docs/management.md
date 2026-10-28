# Cổng Quản lý (trụ sở)

Khu vực làm việc của vai trò `QUAN_LY` (Quản trị viên cũng gọi được các API này): danh mục và tựa sách, tồn kho chuỗi, kiểm duyệt, tiền, voucher trụ sở, đối tác vận chuyển, chiết khấu. Quản trị viên lo phần hệ thống (xem [admin.md](admin.md)).

## Khung trang

- Mọi trang dùng chung `templates/fragments/manager/layout.html`: sidebar trái, thanh trên (tiêu đề trang, làm mới, đổi giao diện, chuông thông báo, nút sang trang chuỗi).
- Màu và bố cục ở `static/css/manager.css`, mọi màu là biến `--mg-*` dưới `body.manager-shell`; `manager-admin.css` dùng thêm cho các trang có bảng quản trị. Giao diện dùng chung lựa chọn với trang khách (`static/js/theme.js`).
- Từ trang khách quay lại bằng mục **"Về cổng quản lý"** trong menu tài khoản.

## Chuyển trang không tải lại

`static/js/management/shell.js` bắt click trên link có `data-shell-link`:

1. Tải HTML trang đích, chỉ lấy `<main id="managerMain">` và `<title>`, thay vào trang hiện tại, đổi địa chỉ bằng `history.pushState`.
2. Chạy lại các `<script data-page-script>` của trang đích.
3. Phát sự kiện `manager:leave` trước khi thay nội dung để trang cũ dọn hẹn giờ, bỏ đăng ký.
4. Lỗi mạng, hoặc trang trả về không có `#managerMain` (ví dụ bị đẩy ra trang đăng nhập), thì tải lại trang bình thường.

Thẻ con trong một trang (ví dụ 3 thẻ của Thanh toán) ghi vào `?tab=` để F5 vẫn mở đúng thẻ.

## Các trang

| Trang | Địa chỉ | Nội dung |
|---|---|---|
| Tổng quan | `/management` | Thẻ chỉ số (chờ duyệt, hoàn tiền chờ xử lý, chi nhánh chờ chi, chiết khấu tháng, giao dịch lệch 30 ngày), bấm vào mở đúng hàng đợi; 5 việc mới nhất |
| Thống kê | `/management/stats` | Theo kỳ (tối đa 366 ngày, mặc định 30 ngày): dòng tiền, theo chi nhánh, đơn chờ xác nhận quá 48 giờ (có nút nhắc), theo đơn vị vận chuyển, hủy và hoàn, theo danh mục |
| Danh mục & tựa sách | `/management/catalog` | Xem [catalog-store.md](catalog-store.md#3-trụ-sở-danh-mục-và-tựa-sách) |
| Tồn kho chuỗi | `/management/inventory` | Tra tồn theo tựa sách, phiếu điều chuyển (xem [branch-inventory.md](branch-inventory.md)) |
| Thanh toán & Đối soát | `/management/payments?tab=payouts\|refunds\|transactions` | Chi cho chi nhánh, hoàn tiền cho khách, sổ giao dịch ngân hàng |
| Kiểm duyệt | `/store-moderation` | Duyệt chi nhánh và sách |
| Voucher | `/management/vouchers` | Voucher trụ sở (xem [voucher.md](voucher.md)) |
| Đơn vị vận chuyển | `/management/shipping-carriers` | Thêm, sửa, tạm dừng đối tác (xem [shipping.md](shipping.md)) |
| Chiết khấu | `/management/commissions` | Tỷ lệ chiết khấu theo danh mục, kèm lịch sử thay đổi |
| Hồ sơ | `/management/profile` | Thông tin cá nhân, ảnh đại diện |

## Thanh toán & Đối soát

### Chi cho chi nhánh (`tab=payouts`)

- Khoản chi (`yeu_cau_chuyen_tien`, loại `CHI_CUA_HANG`) được lập khi một đơn con **giao thành công** (cả COD lẫn chuyển khoản), số tiền = `doanhThuShop`.
- Danh sách gom theo chi nhánh; mở dòng để xem và tích từng đơn. Mỗi lần chỉ tích được đơn của **một** chi nhánh.
- **Tạo đợt chi VietQR:** máy chủ tính tổng, sinh **mã đợt chi** `DC` + 8 ký tự từ danh sách khoản và tổng tiền, trả mã VietQR tới tài khoản chi nhánh với nội dung là mã đợt chi. Quản lý chuyển khoản rồi bấm **Xác nhận đã chuyển**; máy chủ tính lại mã, khớp thì đánh dấu cả đợt `DA_CHUYEN` trong một giao dịch, ghi sổ `thanh_toan` và báo cho chi nhánh.
- Trong lúc đó nếu có khoản bị đổi hoặc đã được Quản lý khác đánh dấu thì mã không còn khớp và việc xác nhận bị từ chối ("Đợt chi đã thay đổi, vui lòng tạo lại mã đợt chi").
- Chi nhánh chưa khai tài khoản thì có nút **Gửi thông báo cập nhật STK** (mỗi chi nhánh tối đa một lần trong 1 giờ).

### Hoàn tiền cho khách (`tab=refunds`)

- Khoản hoàn (`HOAN_TIEN_KHACH`) được lập khi đơn chuyển khoản bị hủy, hoàn hàng, hoặc tiền về mà không tạo được đơn.
- Tài khoản nhận được điền sẵn theo thứ tự: tài khoản khách chọn lúc hủy đơn, rồi tài khoản đã chuyển tiền (Casso gửi kèm). Có tài khoản thì khoản vào thẳng `CHO_XU_LY`, chưa có thì `CHO_THONG_TIN` và Quản lý có nút nhắc khách.
- Lọc theo trạng thái và từ khóa (mã đơn, tên, SĐT, email khách).

### Giao dịch ngân hàng (`tab=transactions`)

Sổ mọi giao dịch Casso đã nhận, lọc theo từ khóa, kết quả đối soát (`KHOP`, `CAN_HOAN_TIEN`, `KHONG_KHOP`, `DANG_XU_LY`) và khoảng ngày, kèm số giao dịch, tiền khớp đơn, tiền lệch.

Luồng tiền tổng thể xem [payment.md](payment.md).

## Chiết khấu

- Mỗi danh mục có một cấu hình `HOAT_DONG` trong `cau_hinh_chiet_khau`; chưa có thì dùng mặc định 5% (`marketplace.commission.default-rate`).
- Sửa tỷ lệ (0–100%, ghi chú ≤ 500 ký tự, không bắt buộc): đóng bản ghi đang hiệu lực (`ngay_ket_thuc`), thêm bản ghi mới có người sửa (`ma_tk_cap_nhat`), ghi nhật ký hệ thống. Tỷ lệ mới trùng tỷ lệ đang áp dụng thì bị từ chối.
- Tỷ lệ được lấy **lúc đơn giao thành công**, nên % mới áp cho các đơn giao thành công sau thời điểm lưu.
- Cơ sở tính chiết khấu là thành tiền sau khi trừ phần voucher chi nhánh (xem [voucher.md](voucher.md#4-ai-chịu-tiền-giảm)).

## Thống kê và liên kết với Admin

`service/management/OperationsStatsService` chỉ đọc, dùng chung cho trang Thống kê của Quản lý và báo cáo của Admin (`/api/admin/reports/dong-tien`, `/api/admin/reports/theo-danh-muc`), nên con số hai bên luôn khớp. Chiết khấu tính trên đơn `DA_GIAO`, theo ngày giao; tỉ lệ theo chi nhánh và theo hãng tính trên các đơn đặt trong kỳ.

Nhắc chi nhánh xác nhận các đơn chờ quá 48 giờ: mỗi chi nhánh tối đa một lần mỗi giờ.

Quản lý xem để xử lý việc; Admin chỉ xem và xuất CSV.

## Cập nhật thời gian thực

Dùng kênh WebSocket `/ws/notifications` qua `static/js/notifications.js`. `static/js/management/realtime.js` giữ **một** listener và phân phối cho trang đang mở, gộp các tin đến trong 1 giây thành một lần tải lại, đồng thời làm mới số trên sidebar (`GET /api/management/overview/badges`).

| Tin | Tải lại |
|---|---|
| Thông báo `GIAN_HANG_CHO_DUYET`, `SACH_CHO_DUYET` | Kiểm duyệt, Tổng quan |
| Thông báo `CAN_HOAN_TIEN` | Thẻ Hoàn tiền, Tổng quan |
| Thông báo `CAN_CHI_CUA_HANG` | Thẻ Chi cho chi nhánh, Tổng quan |
| Thông báo `GIAO_DICH_LECH` | Thẻ Giao dịch ngân hàng, Tổng quan |
| Tin `VAN_HANH_THAY_DOI` kèm `phan` (`payouts`, `refunds`, `transactions`, `commissions`, `moderation`) | Phần ghi trong `phan` |

`VAN_HANH_THAY_DOI` không lưu vào bảng `thong_bao`; `OperationsPusher` gửi tới mọi Quản lý đang mở trang sau khi giao dịch commit (khách khai tài khoản nhận, một Quản lý bấm "Đã chuyển", đổi chiết khấu...). Nhờ vậy hai Quản lý mở cùng lúc đều thấy thay đổi mà không phải F5.

## API

| Method | URL | Việc |
|---|---|---|
| GET | `/api/management/overview` | Số liệu Tổng quan |
| GET | `/api/management/overview/badges` | Số trên sidebar |
| GET | `/api/management/stats?tuNgay&denNgay` | Thống kê vận hành (ngày `yyyy-MM-dd`) |
| POST | `/api/management/stats/stores/{maCH}/remind-confirm` | Nhắc chi nhánh xác nhận đơn quá 48 giờ |
| GET | `/api/management/payments/store-payouts?trangThai` | Khoản chi gom theo chi nhánh (`CHO_XU_LY` mặc định, hoặc `DA_CHUYEN`) |
| POST | `/api/management/payments/store-payouts/preview` | `{ "maYeuCau": [..] }` → mã đợt chi, tổng tiền, mã VietQR |
| POST | `/api/management/payments/store-payouts/done` | `{ "maDotChi": "DC...", "maYeuCau": [..] }` → đánh dấu cả đợt |
| POST | `/api/management/payments/stores/{maCH}/remind-bank` | Nhắc chi nhánh khai tài khoản |
| POST | `/api/management/payments/refunds/{maYeuCau}/remind-bank` | Nhắc khách khai tài khoản |
| GET | `/api/management/payments/transfers?loai&trangThai&tuKhoa&page&size` | Danh sách khoản hoàn (`HOAN_TIEN_KHACH`) hoặc khoản chi (`CHI_CUA_HANG`) |
| POST | `/api/management/payments/transfers/{maYeuCau}/done` | Đánh dấu một khoản đã chuyển |
| GET | `/api/management/payments/transactions?tuKhoa&ketQua&tuNgay&denNgay&page&size` | Sổ giao dịch ngân hàng |
| GET | `/api/management/commissions` | Tỷ lệ chiết khấu theo danh mục |
| PUT | `/api/management/commissions/{maDM}` | Đổi tỷ lệ |
| GET | `/api/management/commissions/{maDM}/history` | Lịch sử thay đổi |
| — | `/api/management/categories/**`, `/book-titles/**` | Danh mục, tựa sách ([catalog-store.md](catalog-store.md)) |
| — | `/api/management/inventory`, `/transfers` | Tồn kho chuỗi ([branch-inventory.md](branch-inventory.md)) |
| — | `/api/management/vouchers/**` | Voucher trụ sở ([voucher.md](voucher.md)) |
| — | `/api/management/shipping-carriers/**` | Đối tác vận chuyển ([shipping.md](shipping.md)) |

## File liên quan

| File | Vai trò |
|---|---|
| `controller/management/ManagerWebController.java` | Trang Tổng quan, Thống kê, Chiết khấu, Hồ sơ |
| `controller/management/ManagementOperationsController.java` | API Tổng quan, Thống kê, chi gộp, nhắc, chiết khấu |
| `controller/payment/PaymentManagementController.java` | API khoản hoàn/chi, giao dịch ngân hàng |
| `service/management/` | `ManagementOverviewService`, `OperationsStatsService`, `PayoutManagementService`, `CommissionManagementService`, `OperationsPusher` |
| `templates/fragments/manager/layout.html` | Khung trang |
| `templates/management/`, `templates/payment/management.html` | Các trang |
| `static/js/management/` | `shell.js`, `realtime.js`, script từng trang |
| `static/css/manager.css`, `manager-admin.css` | Màu và bố cục |
