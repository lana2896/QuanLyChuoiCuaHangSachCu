# Tài khoản, đăng nhập và phân quyền

## Người dùng, tài khoản và vai trò

- Một **người dùng** (`nguoi_dung`) có thể có nhiều **tài khoản** (`tai_khoan`), mỗi tài khoản đúng một vai trò (`constant/auth/Role.java`):

| Mã vai trò | Tên hiển thị | Có được bằng cách |
|---|---|---|
| `KHACH_HANG` | Khách hàng | Đăng ký trên web |
| `CHU_CUA_HANG` | Chủ chi nhánh | Khách tự đăng ký mở chi nhánh (`POST /api/store/register`) hoặc Quản trị viên cấp |
| `DON_VI_VAN_CHUYEN` | Đơn vị vận chuyển | Không dùng để đăng nhập; đơn vị vận chuyển vào cổng bằng mã (xem [shipping.md](shipping.md)) |
| `QUAN_LY` | Quản lý (trụ sở) | Chỉ Quản trị viên cấp |
| `QUAN_TRI_VIEN` | Quản trị viên | Chỉ Quản trị viên cấp |

- Quản trị viên cấp thêm vai trò bằng `POST /api/admin/accounts/{maTK}/role`: tạo thêm một tài khoản cho cùng người dùng.
- Không có API xóa tài khoản; chỉ khóa và mở khóa.
- Trạng thái tài khoản (`AccountStatus`): `CHO_XAC_THUC`, `HOAT_DONG`, `BI_KHOA`.

## Đăng ký và xác thực OTP

1. `POST /api/auth/register`: tạo người dùng và tài khoản `KHACH_HANG` ở trạng thái `CHO_XAC_THUC`, gửi mã OTP qua Gmail.
2. `POST /api/auth/verify-register-otp`: nhập mã để kích hoạt. Mã sống 5 phút.
3. `POST /api/auth/resend-register-otp`: gửi lại mã; phải chờ 60 giây giữa hai lần gửi.

Quên mật khẩu dùng cùng cơ chế: `POST /api/auth/forgot-password` gửi OTP, `POST /api/auth/resend-forgot-password-otp` gửi lại, `POST /api/auth/reset-password` đặt mật khẩu mới. Đổi mật khẩu khi đã đăng nhập: `POST /api/auth/change-password`.

Mã OTP lưu ở bảng `ma_xac_thuc` (`entity/auth/VerificationCode.java`), phân biệt loại `DANG_KY` và `QUEN_MAT_KHAU`.

Trang tương ứng: `/register`, `/verify-register`, `/forgot-password`, `/change-password`.

## Đăng nhập

1. `POST /api/auth/login` với email và mật khẩu.
2. Người dùng chỉ có một tài khoản đang hoạt động thì nhận `token` (JWT) ngay. Có nhiều tài khoản thì nhận `canChonVaiTro = true`, danh sách `taiKhoans` và một `roleSelectionToken` tạm; gọi `POST /api/auth/select-role` với `{ "maTK": ... }` (kèm token tạm) để nhận JWT chính thức.
3. Trình duyệt lưu JWT trong `localStorage` và gửi `Authorization: Bearer <JWT>` ở mọi lời gọi API (`static/js/auth.js`). Hạn token đặt ở `jwt.expiration` (mặc định 24 giờ).
4. Sau đăng nhập, trang đích theo vai trò: Khách hàng `/`, Chủ chi nhánh `/store/books`, Quản lý `/management`, Quản trị viên `/admin/dashboard`. Đường dẫn `?redirect=` chỉ được chấp nhận nếu là đường dẫn trong cùng trang web (bắt đầu bằng một dấu `/`).

Chuyển sang tài khoản vai trò khác của cùng người dùng mà không đăng nhập lại: `POST /api/auth/switch-account` (menu "Chuyển sang…"). `GET /api/profile` trả thêm `taiKhoans` để giao diện biết có những vai trò nào.

`POST /api/auth/logout` thu hồi token: mã định danh (`jti`) của token được ghi vào danh sách thu hồi (`RevokedTokenService`) đến lúc token hết hạn, nên token đó không dùng lại được.

## Phân quyền

Các trang HTML được phục vụ công khai (chỉ là khung); dữ liệu và thao tác đi qua API có kiểm tra quyền trong `config/auth/SecurityConfig.java`. Phiên làm việc không trạng thái (stateless), CSRF tắt vì xác thực bằng header.

| API | Vai trò |
|---|---|
| Đăng ký, OTP, đăng nhập, chọn vai trò, quên mật khẩu | Ai cũng gọi được |
| `GET /api/books/**`, `GET /api/branches/**`, `GET /api/locations/**` | Ai cũng gọi được |
| `POST /api/payment/casso/webhook` | Không cần đăng nhập; bắt buộc chữ ký HMAC-SHA512 hợp lệ |
| `/api/payment/**` (còn lại) | `KHACH_HANG` |
| `/api/wishlist/**`, `/api/recently-viewed/**`, `/api/reviews/**`, `/api/vouchers/**` | `KHACH_HANG` |
| `POST /api/store/register` | `KHACH_HANG` |
| `/api/store/moderation/**` | `QUAN_LY`, `QUAN_TRI_VIEN` |
| `/api/store/**` (còn lại, gồm `/api/store/transfers`, `/api/store/vouchers`) | `CHU_CUA_HANG` |
| `/api/management/**` | `QUAN_LY`, `QUAN_TRI_VIEN` |
| `/api/admin/**` | `QUAN_TRI_VIEN` |
| `/api/shipping/{maCode}/**` | Không cần đăng nhập; dữ liệu giới hạn theo mã đơn vị (xem [shipping.md](shipping.md)) |
| `/ws/**` | Bắt tay WebSocket bằng vé dùng một lần (`POST /api/notifications/ticket`), không gửi JWT |
| Mọi API còn lại (`/api/cart/**`, `/api/orders/**`, `/api/profile/**`, `/api/notifications/**`...) | Cần đăng nhập; nghiệp vụ tự kiểm tra vai trò (ví dụ chỉ Khách hàng được đặt hàng) |

Lỗi 401 (chưa đăng nhập hoặc token hết hạn) đưa người dùng về `/login` kèm đường dẫn quay lại; lỗi 403 hiện thông báo thiếu quyền ngay trên trang. Cả hai lỗi trả JSON dạng `ApiResponse` với thông báo tiếng Việt.

Chủ chi nhánh chỉ thao tác trên chi nhánh của mình: `security/store/StoreAccess.java` lấy chi nhánh từ JWT, không nhận mã chi nhánh từ trình duyệt.

## Tài khoản bị khóa

Quản trị viên khóa (`PUT /api/admin/accounts/{maTK}/lock`) hoặc mở khóa (`.../unlock`) từng tài khoản. Tài khoản bị khóa không đăng nhập được; các tài khoản vai trò khác của cùng người dùng không bị ảnh hưởng. Mọi thao tác tạo tài khoản, khóa, mở khóa, cấp vai trò đều ghi nhật ký hệ thống.

## File liên quan

| File | Vai trò |
|---|---|
| `controller/auth/AuthController.java` | API đăng ký, OTP, đăng nhập, chọn và chuyển vai trò, mật khẩu |
| `controller/auth/AuthWebController.java` | Trang `/login`, `/register`, `/verify-register`, `/forgot-password`, `/profile`, `/addresses`, `/change-password` |
| `service/auth/AuthService.java`, `JwtService.java` | Nghiệp vụ xác thực, cấp và kiểm tra JWT |
| `service/auth/RevokedTokenService.java`, `AccountTokenService.java` | Thu hồi token khi đăng xuất, cấp token khi chuyển tài khoản |
| `service/auth/EmailService.java` | Gửi OTP qua Gmail |
| `filter/auth/JwtAuthenticationFilter.java` | Đọc header `Authorization`, kiểm tra token |
| `config/auth/SecurityConfig.java` | Luật phân quyền theo đường dẫn |
| `controller/identity/AdminAccountController.java` | Quản trị viên tạo tài khoản, cấp vai trò, khóa/mở khóa |
| `static/js/auth.js` | Lưu token, gắn header, menu theo vai trò, chuyển tài khoản |

## Kiểm tra thủ công

Đăng ký → nhận OTP → xác thực → đăng nhập → Quản trị viên cấp thêm vai trò Quản lý cho người đó → đăng nhập lại, thấy bước chọn vai trò → chọn Quản lý, vào `/management` → chuyển sang Khách hàng qua menu → đăng xuất, gọi lại API bằng token cũ phải nhận 401.
