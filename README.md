# OldBook – Chuỗi cửa hàng sách cũ

> **Đề tài:** Xây dựng website quản lý chuỗi cửa hàng sách cũ  
> **Đồ án môn học:** Lập Trình Web (Học kỳ I, năm học 2026–2027), mã lớp WEPR330479_11
> **Đơn vị đào tạo:** Khoa Công nghệ thông tin, Trường Đại học Công nghệ Kỹ thuật TP. Hồ Chí Minh  
> **Giảng viên hướng dẫn:** ThS. Nguyễn Hữu Trung 
> **Nhóm thực hiện:** Nhóm 21

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-blue)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-purple)
![SQL Server](https://img.shields.io/badge/Database-SQL%20Server-red)

---

## Thông tin đồ án

| | |
|---|---|
| Trường | Đại học Công nghệ Kỹ thuật TP.HCM, Khoa Công nghệ thông tin |
| Môn học | Lập Trình Web (mã lớp WEPR330479_11) |
| Học kỳ | Học kỳ I, năm học 2026 – 2027 |
| Giảng viên hướng dẫn | ThS. Nguyễn Hữu Trung |

## Thành viên nhóm

| STT | Họ và tên | MSSV | Liên hệ (Email) |
| :---: | :--- | :--- | :--- |
| 1 | Nguyễn Ngọc Minh Thư | 24110347 | 24110347@student.hcmute.edu.vn |
| 2 | Trần Ngọc Tuyên | 24110373 | 24110373@student.hcmute.edu.vn |

---

## Giới thiệu chung

**OldBook** là website bán sách cũ của **một chuỗi cửa hàng** gồm nhiều chi nhánh, xây dựng bằng **Spring Boot**, **Thymeleaf** và **Bootstrap 5**. Trụ sở quản lý danh mục và **tựa sách** dùng chung; mỗi chi nhánh nhập các **bản sách cũ** của tựa đó vào kho của mình (độ mới, tình trạng, số lượng), giá bán tự tính theo giá gốc và độ mới. Khách đặt một đơn gồm sách của nhiều chi nhánh, hệ thống tách thành đơn con cho từng chi nhánh. Tiền khách trả về **tài khoản trụ sở**; khi đơn giao thành công, trụ sở trừ chiết khấu theo danh mục rồi chi lại doanh thu cho chi nhánh. Các chi nhánh có thể **điều chuyển sách** cho nhau.

Hệ thống có sáu vai trò: **Khách vãng lai**, **Khách hàng**, **Chủ chi nhánh**, **Đơn vị vận chuyển**, **Quản lý** (trụ sở) và **Quản trị viên**. Dự án tích hợp **thanh toán chuyển khoản VietQR** (xác nhận tiền về qua Casso), **voucher của trụ sở và của chi nhánh**, **thông báo thời gian thực bằng WebSocket**, **xác thực OTP qua email** và **lưu ảnh, video trên Cloudinary**.

---

## Cấu trúc thư mục

```bash
QuanLyChuoiCuaHangSachCu/
├── src/
│   ├── main/
│   │   ├── java/com/oldbook/
│   │   │   ├── OldbookApplication.java
│   │   │   ├── config/               # Bảo mật, WebSocket, thanh toán, vận chuyển, Cloudinary
│   │   │   ├── controller/           # API REST (/api/**) và controller trả trang HTML
│   │   │   │   ├── admin/            # Quản trị viên: dashboard, người dùng, nhật ký, báo cáo
│   │   │   │   ├── auth/             # Đăng nhập, đăng ký, OTP, quên mật khẩu
│   │   │   │   ├── branch/           # Hệ thống chi nhánh (trang công khai)
│   │   │   │   ├── cart/             # Giỏ hàng
│   │   │   │   ├── catalog/          # Sách, tìm kiếm, danh mục, tựa sách, đánh giá, yêu thích
│   │   │   │   ├── identity/         # Hồ sơ, địa chỉ, tài khoản và vai trò
│   │   │   │   ├── inventory/        # Điều chuyển sách giữa chi nhánh, tồn kho toàn chuỗi
│   │   │   │   ├── location/         # Tỉnh/thành, phường/xã
│   │   │   │   ├── management/       # Cổng Quản lý: tổng quan, thống kê, chi trả, chiết khấu
│   │   │   │   ├── notification/     # Thông báo
│   │   │   │   ├── order/            # Đặt hàng, đơn của chi nhánh
│   │   │   │   ├── payment/          # VietQR, webhook Casso, hoàn tiền, voucher
│   │   │   │   ├── shipping/         # Đơn vị vận chuyển và cổng đối tác
│   │   │   │   └── store/            # Chi nhánh, sách của chi nhánh, kiểm duyệt
│   │   │   ├── service/              # Nghiệp vụ, chia phân hệ như controller (thêm media/)
│   │   │   ├── entity/               # Entity JPA (35 bảng)
│   │   │   ├── repository/           # Spring Data JPA
│   │   │   ├── dto/  constant/  exception/  filter/  security/
│   │   ├── resources/
│   │   │   ├── templates/
│   │   │   │   ├── admin/            # Trang Quản trị viên
│   │   │   │   ├── management/       # Cổng Quản lý
│   │   │   │   ├── payment/          # Quét mã, hoàn tiền, quản lý thanh toán
│   │   │   │   ├── shipping/         # Cổng đơn vị vận chuyển, quản lý đối tác
│   │   │   │   ├── branches/         # Danh sách và chi tiết chi nhánh
│   │   │   │   ├── auth/  books/
│   │   │   │   └── fragments/        # Header, footer, khung trang dùng chung cho từng khu vực
│   │   │   ├── static/
│   │   │   │   ├── css/              # style.css, manager.css, manager-admin.css, admin-theme.css
│   │   │   │   ├── images/           # Ảnh banner trang chủ
│   │   │   │   └── js/               # JavaScript từng trang (admin/, management/, shipping/)
│   │   │   └── application.properties
│   └── test/java/                    # Test Spring Boot
├── docs/                             # Tài liệu từng phân hệ
│   └── payment/                      # Script giả lập Casso đi kèm payment.md
├── .env.example                      # Mẫu cấu hình (chép thành .env)
├── pom.xml
├── mvnw / mvnw.cmd
└── README.md
```

---

## Tính năng chính

- Tìm kiếm, lọc sách cũ theo danh mục, chi nhánh, giá, độ mới, tình trạng, đánh giá
- Xem hệ thống chi nhánh theo tỉnh/thành; xem cùng một tựa sách đang có ở những chi nhánh nào
- Giỏ hàng, sách yêu thích, sách đã xem gần đây
- Đặt một đơn gồm sách của nhiều chi nhánh; thanh toán COD hoặc chuyển khoản VietQR
- Voucher của trụ sở (áp cho cả đơn) và voucher của chi nhánh (áp cho đơn con)
- Theo dõi đơn hàng, hủy đơn, đánh giá kèm ảnh và video
- Chi nhánh quản lý kho sách, đơn hàng, voucher riêng; điều chuyển sách sang chi nhánh khác
- Trụ sở quản lý danh mục, tựa sách, kiểm duyệt chi nhánh và sách, tra tồn kho toàn chuỗi
- Cổng riêng cho đơn vị vận chuyển cập nhật trạng thái giao hàng
- Cổng Quản lý: tổng quan, thống kê vận hành, chiết khấu theo danh mục, hoàn tiền và chi tiền cho chi nhánh
- Trang Quản trị: tài khoản và vai trò, nhật ký hệ thống, báo cáo doanh thu toàn chuỗi
- Thông báo thời gian thực qua WebSocket
- Xác thực tài khoản bằng OTP qua email, bảo mật bằng JWT

---

## Tính năng chi tiết

### 1. Trang chủ, chi nhánh và mua sắm

- Tìm kiếm theo tên sách, tác giả; lọc theo danh mục, chi nhánh, khoảng giá, độ mới, tình trạng, còn hàng, số sao
- Chi tiết sách: nhiều ảnh, thông tin chi nhánh bán, đánh giá của người mua và danh sách **các chi nhánh khác đang có cùng tựa sách** (giá, độ mới, tồn kho)
- Trang `/branches`: các chi nhánh đang hoạt động, lọc theo tỉnh/thành, số sách đang bán ở từng nơi
- Thêm vào giỏ hàng hoặc danh sách yêu thích; lưu lại sách đã xem

### 2. Đặt hàng và thanh toán

Một đơn có thể gồm sách của nhiều chi nhánh; hệ thống tách thành đơn con để từng chi nhánh xử lý riêng. Mỗi đơn con chọn đơn vị vận chuyển và voucher chi nhánh riêng; voucher trụ sở áp cho cả đơn.

- **COD:** đơn vị vận chuyển thu hộ khi giao
- **Chuyển khoản VietQR:** hệ thống giữ sách 5 phút và hiện mã QR tới tài khoản trụ sở; đơn chỉ được tạo khi Casso báo tiền về
- Hủy đơn hoặc gửi yêu cầu hủy; đơn đã trả tiền được hoàn về tài khoản khách đã dùng để chuyển, hoặc tài khoản khách chọn

### 3. Chi nhánh (Chủ chi nhánh)

- Khách hàng đăng ký mở chi nhánh; tên chi nhánh do hệ thống đặt theo số thứ tự (`Oldbook chi nhánh <số>`)
- Khai tài khoản ngân hàng để nhận tiền trụ sở chi
- Nhập sách vào kho từ **tựa sách** của trụ sở (tối đa 10 ảnh), cập nhật tồn kho, bật hoặc ngừng bán
- Điều chuyển sách sang chi nhánh khác; nhận hoặc từ chối phiếu điều chuyển gửi tới
- Xác nhận đơn, hủy đơn, xử lý yêu cầu hủy của khách; tạo voucher của chi nhánh

### 4. Vận chuyển (Đơn vị vận chuyển)

- Mỗi đơn vị có cổng riêng tại `/shipping/{mã đơn vị}` (sẵn có GHN, GHTK, VTP, JT)
- Tiếp nhận đơn, cập nhật hành trình, giao thành công hoặc giao thất bại
- Trạng thái cập nhật tới khách và chi nhánh theo thời gian thực

### 5. Cổng Quản lý (trụ sở)

- **Tổng quan:** việc chờ duyệt, khoản cần hoàn, khoản cần chi, chiết khấu tháng, giao dịch lệch
- **Thống kê vận hành:** dòng tiền, theo chi nhánh, theo đơn vị vận chuyển, hủy và hoàn, theo danh mục
- **Danh mục & tựa sách:** thêm, sửa, ngừng bán danh mục và tựa sách dùng chung toàn chuỗi
- **Tồn kho chuỗi:** tra một tựa sách còn bao nhiêu cuốn ở từng chi nhánh, theo dõi phiếu điều chuyển
- **Thanh toán và đối soát:** chi gộp nhiều đơn của một chi nhánh trong một lần chuyển, hoàn tiền cho khách, sổ giao dịch ngân hàng
- **Kiểm duyệt** chi nhánh và sách; **voucher trụ sở**; **đối tác vận chuyển**; **chiết khấu** theo danh mục kèm lịch sử thay đổi

### 6. Quản trị hệ thống (Quản trị viên)

- Tra cứu người dùng; tạo tài khoản, cấp thêm vai trò, khóa và mở khóa
- Nhật ký hệ thống, lọc và xuất CSV
- Báo cáo doanh thu, dòng tiền, doanh thu theo danh mục; chỉ xem và xuất CSV

### 7. Bảo mật

- Xác thực bằng JWT, mật khẩu băm BCrypt; đăng xuất thu hồi token
- Phân quyền theo vai trò ở mọi API; một người dùng có nhiều vai trò thì mỗi vai trò là một tài khoản riêng
- Webhook thanh toán chỉ nhận yêu cầu có chữ ký HMAC-SHA512 hợp lệ
- Thao tác duyệt, khóa tài khoản, đổi chiết khấu đều ghi nhật ký hệ thống

---

## Công nghệ sử dụng

| Thành phần | Công nghệ | Phiên bản | Mô tả |
| :--- | :--- | :--- | :--- |
| **Ngôn ngữ** | Java | 21 | Backend |
| **Framework** | Spring Boot | 4.1.1 | Web MVC, Data JPA, Validation, Mail |
| **Template** | Thymeleaf | 3.1 | Kết xuất trang web |
| **Giao diện** | Bootstrap / Tailwind CSS | 5.3 / CDN | Bootstrap cho trang khách, chi nhánh, cổng Quản lý; Tailwind cho trang Quản trị |
| **Database** | SQL Server | — | Lưu trữ dữ liệu |
| **Bảo mật** | Spring Security | 7 | Xác thực, phân quyền |
| **JWT** | JJWT | 0.12.6 | Token đăng nhập |
| **Lưu ảnh** | Cloudinary | 2.0.0 | Ảnh sách, ảnh đại diện, ảnh và video đánh giá |
| **Thời gian thực** | Spring WebSocket | 4.1.1 | Thông báo, cập nhật đơn hàng và vận chuyển |
| **Thanh toán** | VietQR + Casso | Webhook V2 | Mã QR chuyển khoản, xác nhận tiền về |
| **Email** | Spring Mail (Gmail SMTP) | 4.1.1 | OTP, quên mật khẩu |
| **Build** | Maven Wrapper | — | Không cần cài Maven |

---

## Cài đặt và chạy dự án

### Yêu cầu hệ thống

- Java Development Kit (JDK) 21
- SQL Server
- Node.js 18+ (chỉ để chạy script giả lập thanh toán)
- IDE: IntelliJ IDEA, Eclipse hoặc VS Code

### Hướng dẫn cài đặt

**Bước 1: Lấy mã nguồn**

```bash
git clone <địa chỉ repository>
```

```bash
cd QuanLyChuoiCuaHangSachCu
```

**Bước 2: Tạo database**

Chạy trong SQL Server Management Studio (thay mật khẩu):

```sql
CREATE DATABASE oldbook_db;
GO
CREATE LOGIN oldbook WITH PASSWORD = '<mật khẩu>';
GO
USE oldbook_db;
CREATE USER oldbook FOR LOGIN oldbook;
ALTER ROLE db_datareader ADD MEMBER oldbook;
ALTER ROLE db_datawriter ADD MEMBER oldbook;
ALTER ROLE db_ddladmin ADD MEMBER oldbook;
```

Bảng được tạo tự động ở lần chạy đầu (`spring.jpa.hibernate.ddl-auto=update`). Bốn đơn vị vận chuyển GHN, GHTK, VTP, JT được tạo sẵn khi khởi động (`ShippingDataInitializer`).

**Bước 3: Tạo file `.env`**

Chép `.env.example` thành `.env` ở thư mục gốc rồi điền giá trị. File này được nạp như file properties (`spring.config.import=optional:file:.env[.properties]`) và đã nằm trong `.gitignore`.

| Khóa | Bắt buộc | Ý nghĩa |
| :--- | :--- | :--- |
| `SERVER_PORT` | Có | Cổng chạy web (ví dụ `8080`). `application.properties` **không** có giá trị mặc định, thiếu thì ứng dụng không khởi động |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Có (`DB_URL` có mặc định) | Kết nối SQL Server |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Có | Gmail gửi OTP; dùng **mật khẩu ứng dụng** của Gmail |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Để tải ảnh | Thiếu thì không tải được ảnh |
| `PAYMENT_BANK_BIN`, `PAYMENT_BANK_ACCOUNT_NO`, `PAYMENT_BANK_ACCOUNT_NAME` | Để bật chuyển khoản | Tài khoản ngân hàng của trụ sở; bỏ trống thì chỉ còn COD |
| `PAYMENT_CASSO_CHECKSUM_KEY` | Đi kèm chuyển khoản | Khóa kiểm chữ ký webhook Casso |
| `PAYMENT_SESSION_MINUTES` | Không | Thời gian giữ hàng và mã QR, mặc định 5 phút |

- `jwt.secret` trong `application.properties` chỉ dùng cho máy local; khi deploy phải thay bằng khóa khác.
- Sửa `.env` xong phải khởi động lại ứng dụng.

**Bước 4: Chạy ứng dụng**

```bash
./mvnw spring-boot:run
```

Trên Windows dùng `mvnw.cmd spring-boot:run`, hoặc chạy `OldbookApplication.java` từ IDE.

**Bước 5: Truy cập ứng dụng** (thay `8080` bằng `SERVER_PORT` đã đặt)

| Khu vực | Địa chỉ |
| :--- | :--- |
| Trang khách | http://localhost:8080 |
| Hệ thống chi nhánh | http://localhost:8080/branches |
| Kênh chi nhánh | http://localhost:8080/store/books |
| Cổng Quản lý | http://localhost:8080/management |
| Trang Quản trị | http://localhost:8080/admin/dashboard |
| Cổng đơn vị vận chuyển | http://localhost:8080/shipping/{mã đơn vị}, ví dụ `/shipping/GHN` |

Tài khoản Quản trị viên đầu tiên: đăng ký một tài khoản bình thường trên web, rồi thêm một dòng `tai_khoan` vai trò `QUAN_TRI_VIEN` cho người dùng đó trực tiếp trong database. Sau đó Quản trị viên cấp vai trò cho người khác trên trang **Tài khoản & Vai trò**.

---

## Thanh toán chuyển khoản

Tiền khách trả vào **tài khoản của trụ sở**; đơn chỉ được tạo khi ngân hàng báo tiền về qua Casso. Khi đơn giao thành công, trụ sở trừ chiết khấu rồi Quản lý chi lại cho chi nhánh; đơn hủy hoặc hoàn hàng thì Quản lý hoàn tiền cho khách. Việc chuyển tiền đi là chuyển tay; hệ thống dựng mã VietQR và ghi sổ.

Trên máy local, Casso không gọi tới được, nên dùng script giả lập tiền về:

```bash
node docs/payment/gia-lap-casso.cjs <mã OB...> <số tiền>
```

Hướng dẫn đầy đủ: [docs/payment.md](docs/payment.md). Khi deploy, webhook Casso trỏ tới `https://<địa chỉ web>/api/payment/casso/webhook`.

---

## Kiểm thử

```bash
./mvnw -q test
```

Hiện có `OldbookApplicationTests` kiểm tra ứng dụng khởi động được. Test dùng cấu hình chính nên cần `.env` và SQL Server như khi chạy ứng dụng. Kịch bản kiểm thử thủ công của từng phân hệ nằm cuối mỗi file trong `docs/`.

---

## Tài liệu

| File | Nội dung |
| :--- | :--- |
| [docs/auth.md](docs/auth.md) | Tài khoản, vai trò, đăng ký OTP, đăng nhập, phân quyền |
| [docs/catalog-store.md](docs/catalog-store.md) | Danh mục, tựa sách, chi nhánh, sách của chi nhánh, kiểm duyệt |
| [docs/branch-inventory.md](docs/branch-inventory.md) | Hệ thống chi nhánh, tồn kho toàn chuỗi, điều chuyển sách giữa chi nhánh |
| [docs/order.md](docs/order.md) | Giỏ hàng, đặt hàng, đơn hai cấp |
| [docs/order-cancel.md](docs/order-cancel.md) | Hủy đơn, ảnh hưởng tồn kho và thanh toán |
| [docs/voucher.md](docs/voucher.md) | Voucher trụ sở, voucher chi nhánh, cách tính và phân bổ giảm giá |
| [docs/payment.md](docs/payment.md) | Thanh toán VietQR, hoàn tiền, chi tiền, nối Casso |
| [docs/shipping.md](docs/shipping.md) | Vận chuyển và cổng đối tác |
| [docs/management.md](docs/management.md) | Cổng Quản lý: tổng quan, thống kê, chi gộp, chiết khấu, cập nhật thời gian thực |
| [docs/admin.md](docs/admin.md) | Trang Quản trị: tài khoản, nhật ký, báo cáo, giao diện |
| [docs/frontend-api.md](docs/frontend-api.md) | Trang giao diện và API mỗi trang gọi |

---

## Liên hệ

- GVHD: ThS. Nguyễn Trần Thi Văn
- SVTH: Nhóm 21 - Nguyễn Ngọc Minh Thư 24110347, Trần Ngọc Tuyên 24110373

---

*Đồ án môn Lập Trình Web – Trường Đại học Công nghệ Kỹ thuật TP. Hồ Chí Minh*
