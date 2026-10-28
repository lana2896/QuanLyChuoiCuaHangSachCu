# Danh mục, tựa sách, chi nhánh và kiểm duyệt

Phân hệ quản lý "cái gì được bán ở đâu". Điều chuyển sách giữa chi nhánh và tồn kho toàn chuỗi xem [branch-inventory.md](branch-inventory.md); phân quyền xem [auth.md](auth.md).

## 1. Mô hình dữ liệu

```
danh_muc (trụ sở) ──< tua_sach (trụ sở) ──< sach (từng chi nhánh) >── cua_hang (chi nhánh)
                                              └──< anh_sach (tối đa 10 ảnh)
```

| Khái niệm | Bảng | Ai quản lý | Ghi chú |
|---|---|---|---|
| Danh mục | `danh_muc` | Quản lý | Văn học, Kinh tế, Kỹ năng... Có `trangThaiBan` |
| Tựa sách | `tua_sach` | Quản lý | Tên, tác giả, NXB, năm XB, **giá gốc**, mô tả, ảnh bìa. Dùng chung toàn chuỗi |
| Bản sách | `sach` | Chủ chi nhánh | Một tựa sách ở một chi nhánh, với độ mới (%), tình trạng thực tế, tồn kho, ảnh chụp thật |
| Chi nhánh | `cua_hang` | Chủ chi nhánh, Quản lý duyệt | Mỗi cửa hàng là một chi nhánh của chuỗi |

**Giá bán** của bản sách không nhập tay: `giaBanCu = giaGoc × doMoiPercent / 100`, tính lại mỗi lần lưu (`Book.tinhGiaBan`). Đổi giá gốc của tựa sách thì các bản sách mới lưu sau đó nhận giá mới.

## 2. Trạng thái

| Đối tượng | Trạng thái duyệt (`ApprovalStatus`) | Trạng thái bán (`SaleStatus`) |
|---|---|---|
| Danh mục, tựa sách | — | `DANG_BAN`, `NGUNG_BAN` |
| Chi nhánh | `CHO_DUYET`, `DA_DUYET`, `TU_CHOI` | — |
| Bản sách | `CHO_DUYET`, `DA_DUYET`, `TU_CHOI` (kèm `lyDoTuChoi`) | `DANG_BAN`, `NGUNG_BAN` |

Một bản sách **hiện công khai** khi đồng thời (`BookSpecifications.publicVisible`):

- bản sách `DA_DUYET` và `DANG_BAN`;
- chi nhánh `DA_DUYET`;
- tựa sách và danh mục đều `DANG_BAN`.

Quy tắc liên quan:

- Danh mục đang ngừng bán thì không thêm hoặc chuyển tựa sách vào; muốn bán lại tựa sách phải bán lại danh mục trước.
- Tựa sách hoặc danh mục ngừng bán thì chi nhánh không nhập được bản sách mới và không bật bán lại được.
- Sửa thông tin chi nhánh hoặc nội dung bản sách đưa đối tượng về `CHO_DUYET`. Đổi tài khoản ngân hàng, tồn kho, trạng thái bán **không** cần duyệt lại.

## 3. Trụ sở: danh mục và tựa sách

Trang `/management/catalog` (Quản lý, Quản trị viên).

| Method | URL | Việc |
|---|---|---|
| GET | `/api/management/categories` | Mọi danh mục |
| POST | `/api/management/categories` | Thêm danh mục (tên không trùng) |
| PUT | `/api/management/categories/{maDM}` | Sửa |
| PATCH | `/api/management/categories/{maDM}/sale-status` | `{ "trangThaiBan": "NGUNG_BAN" }` |
| GET | `/api/management/book-titles?tuKhoa&maDM&trangThaiBan&page&size` | Tìm tựa sách, phân trang (size ≤ 100) |
| GET | `/api/management/book-titles/{maTuaSach}` | Chi tiết |
| POST | `/api/management/book-titles` | Thêm tựa sách |
| PUT | `/api/management/book-titles/{maTuaSach}` | Sửa |
| PATCH | `/api/management/book-titles/{maTuaSach}/sale-status` | Bật/ngừng bán |
| POST | `/api/management/book-titles/images` | Tải ảnh bìa (multipart) lên Cloudinary |

Body tựa sách: `maDM`, `tenSach`, `tacGia`, `nhaXuatBan`, `namXuatBan`, `giaGoc`, `moTaChiTiet`, `hinhAnhUrl`.

## 4. Chi nhánh

### 4.1. Đăng ký mở chi nhánh

Trang `/store/register`. Khách hàng gọi `POST /api/store/register` (`StoreService.registerAsCustomer`), trong một giao dịch:

1. tạo thêm tài khoản `CHU_CUA_HANG` cho chính người đó;
2. tạo cửa hàng `CHO_DUYET` với **số chi nhánh** tăng dần toàn chuỗi; **tên do hệ thống đặt**: `Oldbook chi nhánh <số>`;
3. giao diện chuyển sang tài khoản Chủ chi nhánh mới để theo dõi trạng thái duyệt.

Body: `moTa`, `maTinh`, `maXa`, `diaChiChiTiet`, `soDienThoaiCH`, `maNganHang`, `soTaiKhoan`, `tenChuTaiKhoan`. Ba trường ngân hàng đi cùng nhau; form bắt buộc nhập, API cho để trống cả ba để khai sau. Danh sách tỉnh/phường lấy từ `GET /api/locations/provinces` và `/api/locations/provinces/{maTinh}/wards`.

Hai người đăng ký cùng lúc có thể cùng lấy một số chi nhánh; người sau nhận lỗi "Số chi nhánh vừa được cấp cho cửa hàng khác" và gửi lại.

### 4.2. API của Chủ chi nhánh

| Method | URL | Việc |
|---|---|---|
| GET | `/api/store/me` | Chi nhánh của tôi, trạng thái duyệt |
| POST | `/api/store` | Tạo chi nhánh (khi đã có tài khoản Chủ chi nhánh mà chưa có cửa hàng) |
| PUT | `/api/store/me` | Sửa thông tin → về `CHO_DUYET` |
| PUT | `/api/store/me/bank-account` | Đổi tài khoản nhận tiền, không cần duyệt lại |

## 5. Bản sách của chi nhánh

Trang `/store/books`. Chi nhánh phải `DA_DUYET` mới quản lý sách.

| Method | URL | Việc |
|---|---|---|
| GET | `/api/store/me/books/titles?tuKhoa&maDM&page&size` | Tìm tựa sách đang bán để nhập (size ≤ 50) |
| GET | `/api/store/me/books?page&size&sort&trangThaiDuyet` | Sách của tôi; `sort` theo `maSach`, `tenSach`, `giaBanCu`, `soLuongTon`, `ngayTao` kèm `,asc`/`,desc` |
| GET | `/api/store/me/books/{maSach}` | Chi tiết ở mọi trạng thái |
| POST | `/api/store/me/books` | Nhập bản sách → `CHO_DUYET` |
| PUT | `/api/store/me/books/{maSach}` | Sửa độ mới, tình trạng, ảnh → `CHO_DUYET` |
| PATCH | `/api/store/me/books/{maSach}/stock` | Đặt lại tồn kho (số nguyên ≥ 0) |
| PATCH | `/api/store/me/books/{maSach}/sale-status` | `DANG_BAN` / `NGUNG_BAN` |
| POST | `/api/store/me/books/images` | Tải ảnh (multipart) lên Cloudinary, trả URL |

Body nhập sách:

```json
{
  "maTuaSach": 12,
  "doMoiPercent": 85,
  "tinhTrangVatLy": "Gáy hơi sờn, không ghi chú bên trong",
  "soLuongTon": 3,
  "hinhAnhUrls": ["https://res.cloudinary.com/.../a.jpg"]
}
```

Kiểm tra: có ít nhất 1 ảnh, tối đa 10; độ mới trong (0, 100]; tình trạng không trống; tồn kho ≥ 0.

Mọi thao tác ghi khóa chi nhánh trước rồi mới khóa bản sách (cùng thứ tự với kiểm duyệt và điều chuyển) để tránh deadlock.

## 6. Kiểm duyệt (Quản lý)

Trang `/store-moderation`.

| Method | URL | Việc |
|---|---|---|
| GET | `/api/store/moderation/stores` | Lọc chi nhánh theo trạng thái, từ khóa, phân trang (≤ 100) |
| GET | `/api/store/moderation/stores/{maCH}` | Chi tiết |
| POST | `/api/store/moderation/stores/{maCH}/approve` | Duyệt |
| POST | `/api/store/moderation/stores/{maCH}/reject` | `{ "lyDo": "..." }` (≤ 500 ký tự) |
| GET | `/api/store/moderation/books` | Lọc bản sách |
| GET | `/api/store/moderation/books/{maSach}` | Chi tiết |
| POST | `/api/store/moderation/books/{maSach}/approve` | Duyệt; chi nhánh phải đã được duyệt |
| POST | `/api/store/moderation/books/{maSach}/reject` | Từ chối kèm lý do |

- Chỉ duyệt/từ chối được đối tượng đang `CHO_DUYET`.
- Bảng `cua_hang` chưa có cột lý do từ chối; lý do lưu trong nhật ký hệ thống cùng giao dịch.
- Mỗi lần duyệt/từ chối ghi `nhat_ky_he_thong` và gửi thông báo cho Chủ chi nhánh.

## 7. Tra cứu công khai

| Method | URL | Việc |
|---|---|---|
| GET | `/api/books?q&category` | Danh sách đơn giản |
| GET | `/api/books/search` | Tìm có lọc, phân trang |
| GET | `/api/books/{maSach}` | Chi tiết bản sách |
| GET | `/api/books/categories` | Danh mục đang bán |

Tham số `/api/books/search` (`BookSearchRequest`): `tuKhoa` (tên, tác giả, NXB), `maDM`, `maCH` (chi nhánh), `giaTu`/`giaDen` hoặc `khoangGia`, `doMoiTu`/`doMoiDen`, `tinhTrang` (nhiều giá trị), `conHang`, `soSaoTu`, `page` (mặc định 0), `size` (mặc định 12), `sort` (`moi-nhat`, `gia-tang`, `gia-giam`...).

Trang chi tiết `/books/{maSach}` hiện thêm tối đa 8 bản sách **cùng tựa ở các chi nhánh khác** còn hàng, sắp theo giá tăng dần (`BookService.getCopiesAtOtherBranches`).

## 8. File liên quan

| File | Vai trò |
|---|---|
| `controller/catalog/CategoryManagementController.java`, `BookTitleManagementController.java` | API danh mục, tựa sách của trụ sở |
| `controller/catalog/BookController.java`, `BookWebController.java` | API và trang công khai |
| `controller/store/StoreController.java`, `StoreBookController.java`, `StoreModerationController.java` | API chi nhánh, sách của chi nhánh, kiểm duyệt |
| `service/catalog/BookService.java`, `BookTitleManagementService.java`, `CategoryManagementService.java` | Nghiệp vụ tra cứu, danh mục, tựa sách |
| `service/store/StoreService.java`, `StoreBookService.java`, `StoreModerationService.java` | Nghiệp vụ chi nhánh, sách, kiểm duyệt |
| `repository/catalog/BookSpecifications.java` | Điều kiện hiện công khai và bộ lọc tìm kiếm |
| `entity/catalog/Book.java` | Công thức giá bán |
| `templates/store-books.html`, `store-register.html`, `store-moderation.html`, `management/catalog.html` | Giao diện |

## 9. Kiểm tra thủ công

Quản lý tạo danh mục và tựa sách → khách đăng ký mở chi nhánh → Quản lý duyệt chi nhánh → Chủ chi nhánh nhập bản sách từ tựa sách → Quản lý duyệt sách → sách hiện ở `/books` với giá = giá gốc × độ mới → ngừng bán danh mục, sách biến khỏi trang công khai → bán lại danh mục, sách hiện lại.
