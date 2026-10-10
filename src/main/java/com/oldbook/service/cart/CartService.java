package com.oldbook.service.cart;

import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.entity.catalog.Book;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.entity.identity.User;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.dto.cart.AddCartItemRequest;
import com.oldbook.dto.cart.CartItemResponse;
import com.oldbook.dto.cart.CartResponse;
import com.oldbook.dto.cart.UpdateCartItemRequest;
import com.oldbook.entity.cart.CartItem;
import com.oldbook.entity.cart.Cart;
import com.oldbook.repository.cart.CartItemRepository;
import com.oldbook.repository.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Integer maND) {

        return cartRepository.findByMaND(maND)
                .map(this::buildCartResponse)
                .orElseGet(this::emptyCartResponse);
    }

    @Transactional
    public CartResponse addItem(Integer maND, AddCartItemRequest request) {

        Book sach = bookRepository.findById(request.getMaSach())
                .orElseThrow(() -> new BusinessException("Sách không tồn tại"));

        validateBookPurchasable(sach, maND);

        Cart gioHang = getOrCreateCart(maND);

        CartItem chiTiet = cartItemRepository
                .findByGioHangAndSach(gioHang.getMaGioHang(), sach.getMaSach())
                .orElse(null);

        int soLuongHienTai = chiTiet == null ? 0 : chiTiet.getSoLuong();
        int soLuongMoi = soLuongHienTai + request.getSoLuong();

        validateStock(sach, soLuongMoi, soLuongHienTai);

        if (chiTiet == null) {
            chiTiet = CartItem.builder()
                    .gioHang(gioHang)
                    .sach(sach)
                    .soLuong(soLuongMoi)
                    .build();
        } else {
            chiTiet.setSoLuong(soLuongMoi);
        }

        cartItemRepository.save(chiTiet);
        touchUpdatedAt(gioHang);

        return buildCartResponse(gioHang);
    }

    @Transactional
    public CartResponse updateItem(
            Integer maND,
            Integer maCTGioHang,
            UpdateCartItemRequest request
    ) {

        CartItem chiTiet = findMyCartItem(maCTGioHang, maND);

        if (request.getSoLuong() > chiTiet.getSoLuong()) {
            validateBookOnSale(chiTiet.getSach());
            validateStock(chiTiet.getSach(), request.getSoLuong(), 0);
        }

        chiTiet.setSoLuong(request.getSoLuong());
        cartItemRepository.save(chiTiet);

        Cart gioHang = chiTiet.getGioHang();
        touchUpdatedAt(gioHang);

        return buildCartResponse(gioHang);
    }

    @Transactional
    public CartResponse removeItem(Integer maND, Integer maCTGioHang) {

        CartItem chiTiet = findMyCartItem(maCTGioHang, maND);

        Cart gioHang = chiTiet.getGioHang();

        cartItemRepository.delete(chiTiet);
        cartItemRepository.flush();

        touchUpdatedAt(gioHang);

        return buildCartResponse(gioHang);
    }

    @Transactional
    public CartResponse clearCart(Integer maND) {

        Cart gioHang = cartRepository.findByMaND(maND).orElse(null);

        if (gioHang == null) {
            return emptyCartResponse();
        }

        cartItemRepository.deleteAllByGioHang(gioHang.getMaGioHang());
        touchUpdatedAt(gioHang);

        return buildCartResponse(gioHang);
    }

    private Cart getOrCreateCart(Integer maND) {

        return cartRepository.findByMaND(maND)
                .orElseGet(() -> {
                    User nguoiDung = userRepository.findById(maND)
                            .orElseThrow(() ->
                                    new BusinessException("Người dùng không tồn tại")
                            );

                    return cartRepository.save(
                            Cart.builder()
                                    .nguoiDung(nguoiDung)
                                    .build()
                    );
                });
    }

    private CartItem findMyCartItem(Integer maCTGioHang, Integer maND) {

        return cartItemRepository.findByIdAndOwner(maCTGioHang, maND)
                .orElseThrow(() ->
                        new BusinessException("Sản phẩm không có trong giỏ hàng")
                );
    }

    private boolean isOnSale(Book sach) {
        return ApprovalStatus.DA_DUYET.name().equals(sach.getTrangThaiDuyet())
                && SaleStatus.DANG_BAN.name().equals(sach.getTrangThaiBan())
                && ApprovalStatus.DA_DUYET.name().equals(sach.getCuaHang().getTrangThaiDuyet());
    }

    private void validateBookOnSale(Book sach) {
        if (!isOnSale(sach)) {
            throw new BusinessException("Sách hiện không còn được bán");
        }
    }

    private void validateBookPurchasable(Book sach, Integer maND) {

        validateBookOnSale(sach);

        if (sach.getGiaBanCu() == null) {
            throw new BusinessException("Sách chưa có giá bán");
        }

        if (sach.getSoLuongTon() == null || sach.getSoLuongTon() <= 0) {
            throw new BusinessException("Sách đã hết hàng");
        }

        User chuShop = sach.getCuaHang().getChuShop();

        if (chuShop != null && maND.equals(chuShop.getMaND())) {
            throw new BusinessException(
                    "Bạn không thể mua sách của chính cửa hàng mình"
            );
        }
    }

    private void validateStock(Book sach, int soLuongYeuCau, int soLuongDaCoTrongGio) {

        int ton = sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon();

        if (soLuongYeuCau > ton) {

            String message = soLuongDaCoTrongGio > 0
                    ? "Số lượng vượt quá tồn kho. Bạn đã có "
                        + soLuongDaCoTrongGio + " cuốn trong giỏ, cửa hàng chỉ còn "
                        + ton + " cuốn"
                    : "Số lượng vượt quá tồn kho. Cửa hàng chỉ còn " + ton + " cuốn";

            throw new BusinessException(message);
        }
    }

    private void touchUpdatedAt(Cart gioHang) {
        gioHang.setNgayCapNhat(LocalDateTime.now());
        cartRepository.save(gioHang);
    }

    private CartResponse emptyCartResponse() {
        return CartResponse.builder()
                .maGioHang(null)
                .items(new ArrayList<>())
                .soMuc(0)
                .tongSoLuong(0)
                .tongTien(BigDecimal.ZERO)
                .build();
    }

    private CartResponse buildCartResponse(Cart gioHang) {

        List<CartItem> dongs =
                cartItemRepository.findAllByGioHang(gioHang.getMaGioHang());

        List<CartItemResponse> items = new ArrayList<>();
        int tongSoLuong = 0;
        BigDecimal tongTien = BigDecimal.ZERO;

        for (CartItem ct : dongs) {

            Book sach = ct.getSach();

            BigDecimal donGia =
                    sach.getGiaBanCu() == null ? BigDecimal.ZERO : sach.getGiaBanCu();

            BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(ct.getSoLuong()));

            int ton = sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon();

            String canhBao = null;

            if (!isOnSale(sach)) {
                canhBao = "Sách hiện không còn được bán";
            } else if (ton <= 0) {
                canhBao = "Sách đã hết hàng";
            } else if (ct.getSoLuong() > ton) {
                canhBao = "Cửa hàng chỉ còn " + ton + " cuốn";
            }

            boolean coTheMua = canhBao == null;

            items.add(
                    CartItemResponse.builder()
                            .maCTGioHang(ct.getMaCTGioHang())
                            .maSach(sach.getMaSach())
                            .tenSach(sach.getTenSach())
                            .tacGia(sach.getTacGia())
                            .hinhAnhUrl(sach.getHinhAnhUrl())
                            .maCH(sach.getCuaHang().getMaCH())
                            .tenCuaHang(sach.getCuaHang().getTenCuaHang())
                            .donGia(donGia)
                            .soLuong(ct.getSoLuong())
                            .soLuongTon(ton)
                            .thanhTien(thanhTien)
                            .canhBao(canhBao)
                            .coTheMua(coTheMua)
                            .build()
            );

            if (coTheMua) {
                tongSoLuong += ct.getSoLuong();
                tongTien = tongTien.add(thanhTien);
            }
        }

        return CartResponse.builder()
                .maGioHang(gioHang.getMaGioHang())
                .items(items)
                .soMuc(items.size())
                .tongSoLuong(tongSoLuong)
                .tongTien(tongTien)
                .build();
    }
}
