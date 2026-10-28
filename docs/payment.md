# Thanh toán chuyển khoản bằng VietQR

Hướng dẫn bật và thử thanh toán chuyển khoản trên máy local, quy trình hoàn tiền và chi tiền cho chi nhánh, và cách nối Casso khi web đã deploy.

## Chức năng làm gì

Khách chọn "Chuyển khoản bằng mã VietQR" ở trang đặt hàng. Hệ thống giữ sách 5 phút và hiện mã QR tới tài khoản ngân hàng của **trụ sở**. Khi ngân hàng báo tiền về, đơn hàng mới được tạo. Chưa trả tiền thì chưa có đơn và sách vẫn nằm trong giỏ.

| Dòng tiền | Khi nào | Ai thực hiện |
|---|---|---|
| Khách trả trụ sở | Lúc đặt hàng (chuyển khoản), hoặc đơn vị vận chuyển thu hộ khi giao (COD) | Chuyển khoản: tự động, Casso gọi webhook |
| Trụ sở hoàn khách | Đơn chuyển khoản bị hủy, hoàn hàng, hoặc tiền về mà không tạo được đơn | Quản lý chuyển khoản tay rồi bấm "Xác nhận đã chuyển" |
| Trụ sở chi chi nhánh | Đơn con giao thành công, **cả COD lẫn chuyển khoản** (tiền hàng − voucher chi nhánh − chiết khấu) | Quản lý gộp các đơn của một chi nhánh thành một đợt chi, chuyển khoản tay rồi xác nhận |

## 1. Cấu hình

Thêm vào file `.env` ở thư mục gốc (mẫu ở `.env.example`):

```properties
PAYMENT_BANK_BIN=970422
PAYMENT_BANK_ACCOUNT_NO=<số tài khoản của trụ sở>
PAYMENT_BANK_ACCOUNT_NAME=<tên chủ tài khoản>
PAYMENT_CASSO_CHECKSUM_KEY=<một chuỗi ngẫu nhiên>
PAYMENT_SESSION_MINUTES=5
```

| Khóa | Ý nghĩa |
|---|---|
| `PAYMENT_BANK_BIN` | Mã BIN 6 số của ngân hàng theo Napas. Danh sách có trong `static/js/banks.js` |
| `PAYMENT_BANK_ACCOUNT_NO`, `PAYMENT_BANK_ACCOUNT_NAME` | Tài khoản nhận tiền của trụ sở, hiện trên mã QR |
| `PAYMENT_CASSO_CHECKSUM_KEY` | Khóa kiểm chữ ký webhook. Máy local dùng chuỗi ngẫu nhiên (`openssl rand -hex 24`); nối Casso thật thì dùng "Key bảo mật" của Casso |
| `PAYMENT_SESSION_MINUTES` | Thời gian giữ sách và thời gian sống của mã QR |

`application.properties` ánh xạ các khóa này vào `payment.bank.*`, `payment.casso.checksum-key`, `payment.session-minutes`. Cứ 30 giây (`payment.expiry-check-ms`) hệ thống dọn các phiên quá hạn.

Lưu ý:

- **Bỏ trống ba khóa `PAYMENT_BANK_*` thì trang đặt hàng chỉ còn COD.** Máy chưa cấu hình vẫn chạy bình thường.
- **Sửa `.env` xong phải khởi động lại ứng dụng.**
- **Đừng quét mã chuyển tiền thật khi còn dùng số tài khoản thử:** tiền sẽ đi vào tài khoản của người lạ.

Bảng liên quan: `phien_thanh_toan`, `giao_dich_ngan_hang`, `yeu_cau_chuyen_tien`, `thanh_toan`; ba cột tài khoản ngân hàng trên `cua_hang`.

## 2. Phiên thanh toán

```
Khách bấm Đặt hàng (chuyển khoản)
   └─► POST /api/payment/sessions ─► kiểm tra như đặt hàng thường, trừ tồn kho để giữ sách
          phiên CHO_THANH_TOAN, mã OB + 8 ký tự (không có 0, 1, I, O)
             ├─ Casso báo đủ tiền ──────► DA_THANH_TOAN, tạo đơn CHUYEN_KHOAN đã thanh toán
             ├─ quá hạn ────────────────► HET_HAN, trả sách về kho
             ├─ khách bấm Hủy ──────────► DA_HUY, trả sách về kho
             └─ tiền về nhưng không tạo được đơn ─► CAN_HOAN_TIEN, lập khoản hoàn
```

| Method | URL | Việc |
|---|---|---|
| GET | `/api/payment/config` | Trụ sở đã bật chuyển khoản chưa, thời gian phiên |
| POST | `/api/payment/sessions` | Tạo phiên (body giống `POST /api/orders`, xem [order.md](order.md)) |
| GET | `/api/payment/sessions/{maPhien}` | Trạng thái phiên, mã QR |
| DELETE | `/api/payment/sessions/{maPhien}` | Khách hủy phiên |
| POST | `/api/payment/casso/webhook` | Casso báo giao dịch (header `X-Casso-Signature`) |

Khách bấm thanh toán lại thì phiên chờ cũ nhả sách trước, để chính họ không bị báo hết hàng. Trang `/payment/pay` nhận cập nhật trạng thái phiên qua WebSocket nên tự chuyển trang khi tiền về.

Xử lý một giao dịch Casso (`PaymentConfirmationService`):

| Tình huống | Kết quả (`ReconciliationResult`) |
|---|---|
| Casso gửi lại giao dịch đã xử lý xong | Bỏ qua |
| Không phải tiền vào, hoặc nội dung không có mã `OB…` | `KHONG_KHOP`, chỉ ghi sổ |
| Đủ tiền, tạo được đơn | `KHOP` |
| Thiếu tiền, hết hàng, giá hoặc giỏ đã đổi | `CAN_HOAN_TIEN`, hoàn đúng số đã nhận |
| Phiên đã được trả bằng giao dịch khác | `CAN_HOAN_TIEN`, hoàn giao dịch thừa |

Phiên đã hết hạn hoặc đã hủy mà tiền vẫn về thì vẫn thử tạo đơn: còn hàng là khách có đơn. Nội dung chuyển khoản được bỏ ký tự lạ trước khi dò mã, vì ngân hàng hay chèn thêm chữ.

## 3. Thử trên giao diện

Cần: một **khách hàng** có địa chỉ giao hàng, một **chi nhánh đã duyệt** có sách đang bán, và một **Quản lý**.

### 3.1. Khách trả tiền

1. Đăng nhập khách, thêm sách vào giỏ, mở `/checkout`.
2. Chọn **Chuyển khoản bằng mã VietQR** rồi bấm **Đặt hàng**.
3. Trang quét mã hiện mã QR, số tiền, nội dung `OBxxxxxxxx` và đồng hồ đếm ngược. Chưa có đơn; sách vẫn trong giỏ nhưng đã được giữ.
4. Giả lập ngân hàng báo tiền về (cửa sổ lệnh khác, ở thư mục gốc):

   ```bash
   node docs/payment/gia-lap-casso.cjs OBxxxxxxxx 275000
   ```

   Thay mã và số tiền bằng đúng giá trị trên trang. Script đọc cổng từ `SERVER_PORT` trong `.env`; muốn chỉ định cổng khác thì thêm ở cuối lệnh.
5. Trong vài giây trang tự chuyển sang **Đặt hàng thành công**. Đơn có phương thức "Chuyển khoản", trạng thái "Đã thanh toán".

### 3.2. Các tình huống khác

| Thử | Cách làm | Kết quả |
|---|---|---|
| Hết hạn | Để trang quét mã quá 5 phút | Trang báo mã hết hạn; sách bán lại được |
| Hủy | Bấm **Hủy thanh toán** | Sách hết bị giữ, vẫn nằm trong giỏ |
| Chuyển thiếu tiền | Chạy script với số tiền nhỏ hơn | Không có đơn; khách có một khoản hoàn bằng đúng số đã chuyển |
| Tiền về muộn | Chạy script sau khi mã hết hạn | Còn sách thì vẫn tạo đơn; hết sách thì thành khoản hoàn |
| Trả hai lần | Chạy script hai lần cùng mã | Một đơn; giao dịch sau thành khoản hoàn |

### 3.3. Hoàn tiền cho khách

Khoản hoàn (`TransferType.HOAN_TIEN_KHACH`, nội dung chuyển khoản `HT<mã>`) có trạng thái `TransferStatus`:

```
CHO_THONG_TIN (chưa biết tài khoản nhận) ──khách khai──► CHO_XU_LY ──Quản lý chuyển và xác nhận──► DA_CHUYEN
```

Tài khoản nhận được điền sẵn theo thứ tự: tài khoản khách chọn lúc hủy đơn → tài khoản người chuyển trong giao dịch ngân hàng. Có sẵn thì khoản hoàn vào thẳng `CHO_XU_LY`.

1. Khách mở `/orders`, hủy đơn đã trả tiền (lý do 50–500 ký tự). Hộp thoại cho biết tiền sẽ hoàn về tài khoản đã chuyển; muốn nhận tài khoản khác thì chọn **Hoàn về tài khoản khác** (`PUT /api/payment/orders/{maDH}/refund-account`).
2. Chưa có tài khoản thì khách vào `/payment/refunds` để khai (`PUT /api/payment/refunds/{maYeuCau}/account`). Danh sách: `GET /api/payment/refunds`.
3. Quản lý vào `/management/payments?tab=refunds`, mở mã QR tới tài khoản khách, chuyển khoản rồi bấm **Xác nhận đã chuyển** (`POST /api/management/payments/transfers/{maYeuCau}/done`). Khách nhận thông báo.
4. Khách chưa khai tài khoản thì Quản lý bấm nhắc (`POST /api/management/payments/refunds/{maYeuCau}/remind-bank`, tối đa một lần mỗi giờ).

### 3.4. Chi tiền cho chi nhánh

Khoản chi (`CHI_CUA_HANG`) được lập ngay khi đơn con `DA_GIAO`, ở trạng thái `CHO_XU_LY`, số tiền = doanh thu chi nhánh.

1. Chủ chi nhánh xác nhận đơn; đơn vị vận chuyển cập nhật tới **Đã giao** (`/shipping/GHN`). Đơn trả trước có số tiền thu hộ bằng 0.
2. Quản lý vào `/management/payments?tab=payouts`: khoản chi gom theo chi nhánh.
3. Chi nhánh chưa khai tài khoản thì Quản lý bấm **Gửi thông báo cập nhật STK**; chủ chi nhánh khai ở trang chi nhánh (`PUT /api/store/me/bank-account`, không cần duyệt lại).
4. Quản lý tích các đơn của **một** chi nhánh, bấm **Tạo đợt chi VietQR**: mã QR có tổng tiền và nội dung là mã đợt chi `DC…`. Chuyển khoản rồi bấm **Xác nhận đã chuyển**; cả đợt được đánh dấu cùng lúc. Chi tiết ở [management.md](management.md#chi-cho-chi-nhánh-tabpayouts).

### 3.5. Sổ giao dịch

`/management/payments?tab=transactions` (`GET /api/management/payments/transactions?tuKhoa&ketQua&tuNgay&denNgay`) liệt kê mọi giao dịch Casso đã nhận, kèm kết quả đối soát và tổng tiền khớp/lệch của kết quả lọc.

## 4. Nối Casso thật

Casso chỉ gọi được địa chỉ HTTPS công khai, nên làm bước này sau khi web đã deploy.

1. Người giữ tài khoản ngân hàng của trụ sở đăng ký Casso và liên kết tài khoản đó.
2. Trong Casso: **Kết nối → Tích hợp → Thêm tích hợp → Webhook V2**, chọn tài khoản ngân hàng.
3. Webhook URL: `https://<địa chỉ web>/api/payment/casso/webhook`.
4. Chép **Key bảo mật** Casso sinh ra vào `PAYMENT_CASSO_CHECKSUM_KEY` trên máy chủ, cùng ba biến tài khoản ngân hàng, rồi khởi động lại ứng dụng.
5. Bấm **Gọi thử** trong Casso. Thành công thì giao dịch thử hiện trong sổ giao dịch với kết quả "Không khớp".
6. Mua thử một cuốn sách giá thấp bằng chuyển khoản thật.

Chữ ký: header `X-Casso-Signature: t=<thời gian>,v1=<hex>`, chuỗi ký là `t + "." + JSON nội dung đã sắp khóa theo thứ tự chữ cái`, thuật toán HMAC-SHA512 (`CassoSignatureVerifier`, làm theo mã mẫu Java của Casso). Sai chữ ký trả 401.

## 5. Khi gặp lỗi

| Hiện tượng | Nguyên nhân thường gặp |
|---|---|
| Trang đặt hàng không có lựa chọn chuyển khoản | Thiếu một trong ba khóa `PAYMENT_BANK_*`, chưa khởi động lại sau khi sửa `.env`, hoặc không đăng nhập bằng tài khoản khách |
| Script trả `HTTP 401` | Khóa trong `.env` khác khóa ứng dụng đang dùng; khởi động lại ứng dụng |
| Script báo không gọi được ứng dụng | Sai cổng: kiểm tra `SERVER_PORT` hoặc thêm cổng ở cuối lệnh |
| Script trả 200 nhưng trang quét mã không đổi | Sai mã thanh toán; sổ giao dịch ghi "Không khớp" |
| Tiền về nhưng thành khoản hoàn | Thiếu tiền, giá sách hoặc phí vận chuyển đã đổi, hoặc sách đã hết |
| Trong lúc chờ quét mã, sách trong giỏ hiện "hết hàng" | Đúng thiết kế: tồn kho đang bị trừ tạm để giữ sách |

## 6. Giới hạn

- Hoàn tiền và chi tiền là chuyển khoản tay; Casso chỉ đọc giao dịch, không chuyển tiền được.
- Phí vận chuyển nằm lại ở trụ sở; việc trụ sở trả đơn vị vận chuyển chưa được làm.
- Tiền COD đơn vị vận chuyển thu hộ được coi là đã về trụ sở khi giao thành công; chưa có bước đối soát COD với hãng.

## 7. File liên quan

| File | Vai trò |
|---|---|
| `config/payment/PaymentProperties.java` | Đọc cấu hình |
| `controller/payment/PaymentController.java` | API của khách: cấu hình, phiên, hoàn tiền |
| `controller/payment/CassoWebhookController.java` | Nhận webhook |
| `controller/payment/PaymentManagementController.java` | API Quản lý: khoản chuyển, sổ giao dịch |
| `service/payment/PaymentSessionService.java` | Tạo, hủy, hết hạn phiên; giữ và nhả sách |
| `service/payment/PaymentConfirmationService.java` | Đối soát giao dịch, tạo đơn |
| `service/payment/CassoSignatureVerifier.java` | Kiểm chữ ký |
| `service/payment/TransferService.java` | Lập và xử lý khoản hoàn, khoản chi |
| `service/payment/VietQrLinkBuilder.java` | Dựng link ảnh VietQR |
| `templates/payment/pay.html`, `refunds.html`, `management.html` | Giao diện |
| `docs/payment/gia-lap-casso.cjs` | Script giả lập Casso trên máy local |
