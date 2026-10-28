# Giao diện kết nối API

Giao diện Thymeleaf dùng client `static/js/auth.js` (`window.OldBook`, viết tắt `OB`) để gửi JSON qua `OB.request(method, url, body)`, đọc `ApiResponse.data`, gắn JWT và hiển thị thông báo lỗi từ backend. Các trang chỉ là HTML khung công khai; dữ liệu cá nhân và thao tác quản lý vẫn đi qua API được bảo vệ (xem [auth.md](auth.md)). Phiên hết hạn (401) đưa người dùng về `/login` với đường dẫn quay lại; thiếu quyền (403) hiển thị ngay trên trang.

Mọi API trả về dạng:

```json
{ "code": 200, "message": "Success", "data": { } }
```

Lỗi nghiệp vụ (`BusinessException`) và lỗi kiểm tra dữ liệu trả HTTP 400 với `code: 400`, `data: null` và thông báo tiếng Việt trong `message`; ảnh quá lớn trả 413 (`GlobalExceptionHandler`).

## Khách vãng lai và khách hàng

| Trang | API chính | Ghi chú |
|---|---|---|
| `/`, `/books` | `GET /api/books/search`, `/api/books/categories` | Trang chủ, danh sách có lọc |
| `/books/search` | `GET /api/books/search` | Tìm kiếm nâng cao |
| `/books/{maSach}` | `GET /api/books/{maSach}`, `POST /api/cart/items`, `PUT/DELETE /api/wishlist/{maSach}`, `PUT /api/recently-viewed/{maSach}` | Chi tiết, cùng tựa ở chi nhánh khác, đánh giá |
| `/branches`, `/branches/{maCH}` | `GET /api/branches`, `/api/branches/provinces`, `/api/branches/{maCH}` | Hệ thống chi nhánh ([branch-inventory.md](branch-inventory.md)) |
| `/cart` | `/api/cart/**` | Giỏ hàng. Trên các trang khác, nút giỏ nổi mở ngăn giỏ (`fragments/cart-drawer.html`, `cart-drawer.js`) dùng cùng API |
| `/checkout` | `/api/profile/addresses/**`, `/api/locations/**`, `/api/shipping-carriers`, `/api/vouchers/**`, `/api/payment/config`, `POST /api/orders` hoặc `POST /api/payment/sessions` | Đặt hàng ([order.md](order.md)) |
| `/order-success` | `GET /api/orders/{maDH}` | Đơn vừa tạo |
| `/orders` | `/api/orders/**`, `/api/payment/orders/{maDH}/refund-account`, `POST /api/reviews/order-items/{maCTDH}` | Theo dõi, hủy đơn, đánh giá kèm ảnh/video |
| `/payment/pay` | `/api/payment/sessions/{maPhien}` | Quét mã VietQR ([payment.md](payment.md)) |
| `/payment/refunds` | `GET /api/payment/refunds`, `PUT /api/payment/refunds/{maYeuCau}/account` | Hoàn tiền của tôi |
| `/wishlist` | `GET /api/wishlist` | Sách yêu thích (chỉ sách còn công khai) |
| `/profile` | `GET/PUT /api/profile`, `POST /api/profile/avatar` | Hồ sơ, ảnh đại diện |
| `/addresses` | `/api/profile/addresses/**` | Sổ địa chỉ, đặt mặc định |
| `/store/register` | `POST /api/store/register`, `/api/locations/**` | Đăng ký mở chi nhánh, rồi chuyển sang tài khoản Chủ chi nhánh |

Đăng ký, OTP, quên mật khẩu, đổi mật khẩu có trang riêng: `/register`, `/verify-register`, `/forgot-password`, `/change-password`.

## Chủ chi nhánh

| Trang | API chính |
|---|---|
| `/store`, `/store/books` | `/api/store/me`, `/api/store/me/books/**`, `PUT /api/store/me/bank-account` |
| `/store/orders` | `/api/store/orders/**` |
| `/store/transfers` | `/api/store/transfers/**` |
| `/store/vouchers` | `/api/store/vouchers/**` |

## Quản lý và Quản trị viên

| Trang | API chính |
|---|---|
| `/management/**` | `/api/management/**` ([management.md](management.md)) |
| `/store-moderation` | `/api/store/moderation/**` ([catalog-store.md](catalog-store.md#6-kiểm-duyệt-quản-lý)) |
| `/admin/**` | `/api/admin/**` ([admin.md](admin.md)) |

## Đơn vị vận chuyển

| Trang | API chính |
|---|---|
| `/shipping` | Danh sách cổng của các hãng |
| `/shipping/{maCode}` | `/api/shipping/{maCode}/**`, WebSocket `/ws/shipping?maCode=` ([shipping.md](shipping.md)) |

## Thông báo và thời gian thực

| Method | URL | Việc |
|---|---|---|
| GET | `/api/notifications` | Danh sách thông báo |
| GET | `/api/notifications/unread` | Số chưa đọc |
| PUT | `/api/notifications/{maThongBao}/read` | Đánh dấu đã đọc |
| PUT | `/api/notifications/read-all` | Đọc tất cả |
| POST | `/api/notifications/ticket` | Xin vé dùng một lần để mở WebSocket |

Chuông thông báo trên header (`static/js/notifications.js`) mở `/ws/notifications?ticket=<vé>`. Trình duyệt không gắn được header `Authorization` lúc bắt tay WebSocket, còn đặt JWT lên URL thì lộ trong nhật ký máy chủ, nên dùng vé sống vài chục giây và hủy ngay khi dùng (`NotificationTicketService`). Socket chỉ báo "có tin mới"; dữ liệu vẫn lấy qua REST. Cùng kênh này mang tin `DU_LIEU_THAY_DOI` (đơn hàng, phiên thanh toán đổi) và `VAN_HANH_THAY_DOI` (màn hình Quản lý).

## Script dùng chung

| File | Vai trò |
|---|---|
| `auth.js` | Client API, lưu token, menu theo vai trò, chuyển tài khoản |
| `theme.js` | Giao diện Mặc định / Tối / Tạp chí, đặt `data-theme` trên `<html>` |
| `header-nav.js` | Menu danh mục trên header |
| `notifications.js` | Chuông thông báo, WebSocket |
| `cart.js`, `cart-drawer.js` | Giỏ hàng, nút "Thêm vào giỏ" (`data-add-to-cart`) |
| `location-picker.js`, `banks.js`, `bank-account-form.js` | Chọn tỉnh/phường, danh sách ngân hàng, form tài khoản ngân hàng |
| `book-cards.js`, `book-gallery.js` | Thẻ sách, thư viện ảnh |

## Kiểm tra thủ công

Đăng nhập Quản trị viên → cấp vai trò Quản lý → Quản lý tạo danh mục, tựa sách → khách đăng ký chi nhánh → Quản lý duyệt → Chủ chi nhánh nhập sách → Quản lý duyệt sách → khách tìm, xem sách, thấy các chi nhánh khác có cùng tựa → đặt hàng → chi nhánh xác nhận → hãng giao → Quản lý chi tiền cho chi nhánh. Ở mỗi bước, chuông thông báo của bên liên quan phải đổ ngay mà không cần tải lại trang.
