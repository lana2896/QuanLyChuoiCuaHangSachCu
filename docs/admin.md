# Trang Quản trị

Khu vực của vai trò `QUAN_TRI_VIEN`: tài khoản, nhật ký và báo cáo của hệ thống. Duyệt, tiền, kho và đối tác là việc của Quản lý (xem [management.md](management.md)). Đăng nhập, vai trò và phân quyền xem [auth.md](auth.md).

## Giao diện

- Mọi trang dùng chung `templates/fragments/admin/` (`head`, `sidebar`, `header`, `footer`, `modal`, `notification`), dựng bằng Tailwind CSS (CDN).
- Màu khai báo bằng biến CSS `--ad-*` trong `fragments/admin/head.html`; cấu hình Tailwind đọc các biến này, nên đổi giao diện chỉ cần đổi biến. Giao diện Tối và Tạp chí nằm ở `static/css/admin-theme.css`.
- Bộ chọn giao diện (Mặc định / Tối / Tạp chí) dùng chung lựa chọn với trang khách và cổng Quản lý (`static/js/theme.js`, thuộc tính `data-theme` trên `<html>`).
- Thanh trên có nút sang trang chuỗi; từ trang khách quay lại bằng mục **"Về trang quản trị"** trong menu tài khoản. Sidebar có lối sang cổng Quản lý (cần thêm tài khoản vai trò Quản lý để thao tác).

## Các trang

| Trang | Địa chỉ | Nội dung |
|---|---|---|
| Tổng quan | `/admin`, `/admin/dashboard` | Tổng người dùng, tài khoản theo trạng thái (hoạt động, bị khóa, chờ xác thực), phân bổ vai trò, nhật ký gần đây |
| Người dùng | `/admin/users`, `/admin/users/{maND}` | Tra cứu người dùng, hồ sơ chi tiết, xuất CSV |
| Tài khoản & Vai trò | `/admin/accounts` | Lọc tài khoản, tạo tài khoản, cấp thêm vai trò, khóa và mở khóa |
| Nhật ký hệ thống | `/admin/audit-logs` | Lọc theo từ khóa, hành động, vai trò người thực hiện, địa chỉ IP, khoảng ngày; xuất CSV |
| Báo cáo doanh thu | `/admin/reports` | Doanh thu và chiết khấu theo ngày, dòng tiền toàn chuỗi, doanh thu theo danh mục; chỉ xem và xuất CSV |
| Hồ sơ | `/admin/profile` | Sửa thông tin cá nhân, ảnh đại diện, đổi mật khẩu (`#doi-mat-khau`) |

Báo cáo dòng tiền và theo danh mục dùng chung `OperationsStatsService` với trang Thống kê của Quản lý, nên số liệu hai bên khớp nhau. Admin không có nút thao tác trên tiền.

## API

Mọi API dưới `/api/admin/**` chỉ cho `QUAN_TRI_VIEN`.

| Method | URL | Việc |
|---|---|---|
| GET | `/api/admin/dashboard/summary` | Số liệu Tổng quan |
| GET | `/api/admin/users/{maND}` | Hồ sơ một người dùng |
| GET | `/api/admin/accounts` | Danh sách tài khoản, có lọc |
| POST | `/api/admin/accounts` | Tạo tài khoản |
| POST | `/api/admin/accounts/{maTK}/role` | Cấp thêm vai trò (tạo thêm một tài khoản cho cùng người dùng) |
| PUT | `/api/admin/accounts/{maTK}/lock`, `.../unlock` | Khóa, mở khóa |
| GET | `/api/admin/audit-logs` | Tìm nhật ký, phân trang (`page`, `size` mặc định 20) |
| GET | `/api/admin/audit-logs/summary` | Thống kê nhật ký |
| GET | `/api/admin/audit-logs/export` | Dữ liệu xuất CSV |
| GET | `/api/admin/reports/revenue?tuNgay&denNgay` | Doanh thu và chiết khấu theo ngày |
| GET | `/api/admin/reports/dong-tien?tuNgay&denNgay` | Dòng tiền toàn chuỗi |
| GET | `/api/admin/reports/theo-danh-muc?tuNgay&denNgay` | Doanh thu và chiết khấu theo danh mục |

Ngày dạng `yyyy-MM-dd`, cả hai ngày đều tính vào kỳ. File CSV được tạo ở trình duyệt, có BOM để Excel đọc đúng tiếng Việt.

## Nhật ký hệ thống

Bảng `nhat_ky_he_thong` ghi các thao tác quan trọng: đăng ký chi nhánh; duyệt và từ chối chi nhánh, sách (kèm lý do); tạo tài khoản, cấp vai trò, khóa, mở khóa; đổi chiết khấu. Thao tác duyệt ghi nhật ký trong cùng giao dịch; ghi nhật ký lỗi thì thao tác bị hủy.

## File liên quan

| File | Vai trò |
|---|---|
| `controller/admin/` | Trang HTML (`AdminWebController`) và API dashboard, người dùng, nhật ký, báo cáo |
| `controller/identity/AdminAccountController.java` | API tài khoản và vai trò |
| `service/admin/` | `AdminDashboardService`, `AdminUserService`, `AdminAuditLogService`, `AdminReportService`, `AdminNotificationService` |
| `templates/admin/`, `templates/fragments/admin/` | Các trang và fragment dùng chung |
| `static/js/admin/` | `layout.js` (sidebar, thông báo, hồ sơ) và script từng trang |
| `static/css/admin-theme.css` | Giao diện Tối và Tạp chí |
