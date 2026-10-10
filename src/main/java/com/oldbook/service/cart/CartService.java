package com.oldbook.service.cart;

import com.oldbook.constant.catalog.TrangThaiBan;
import com.oldbook.constant.catalog.TrangThaiDuyet;
import com.oldbook.entity.catalog.Sach;
import com.oldbook.repository.catalog.SachRepository;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.entity.identity.NguoiDung;
import com.oldbook.repository.identity.NguoiDungRepository;
import com.oldbook.dto.cart.AddCartItemRequest;
import com.oldbook.dto.cart.CartItemResponse;
import com.oldbook.dto.cart.CartResponse;
import com.oldbook.dto.cart.UpdateCartItemRequest;
import com.oldbook.entity.cart.ChiTietGioHang;
import com.oldbook.entity.cart.GioHang;
import com.oldbook.repository.cart.ChiTietGioHangRepository;
import com.oldbook.repository.cart.GioHangRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GioHangService {

    private final GioHangRepository gioHangRepository;
    private final ChiTietGioHangRepository chiTietGioHangRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SachRepository sachRepository;

    @Transactional(readOnly = true)
    public CartResponse getCart(Integer maND) {

        return gioHangRepository.findByMaND(maND)
                .map(this::buildCartResponse)
                .orElseGet(this::emptyCartResponse);
    }

    @Transactional
    public CartResponse addItem(Integer maND, AddCartItemRequest request) {

        Sach sach = sachRepository.findById(request.getMaSach())
                .orElseThrow(() -> new BusinessException("Sách không tồn tại"));

        validateSachCoTheMua(sach, maND);

        GioHang gioHang = getOrCreateGioHang(maND);

        ChiTietGioHang chiTiet = chiTietGioHangRepository
                .findByGioHangAndSach(gioHang.getMaGioHang(), sach.getMaSach())
                .orElse(null);

        int soLuongHienTai = chiTiet == null ? 0 : chiTiet.getSoLuong();
        int soLuongMoi = soLuongHienTai + request.getSoLuong();

        validateTonKho(sach, soLuongMoi, soLuongHienTai);

        if (chiTiet == null) {
            chiTiet = ChiTietGioHang.builder()
                    .gioHang(gioHang)
                    .sach(sach)
                    .soLuong(soLuongMoi)
                    .build();
        } else {
            chiTiet.setSoLuong(soLuongMoi);
        }

        chiTietGioHangRepository.save(chiTiet);
        capNhatThoiGian(gioHang);

        return buildCartResponse(gioHang);
    }

    @Transactional
    public CartResponse updateItem(
            Integer maND,
            Integer maCTGioHang,
            UpdateCartItemRequest request
    ) {

        ChiTietGioHang chiTiet = timDongTrongGioCuaToi(maCTGioHang, maND);

        if (request.getSoLuong() > chiTiet.getSoLuong()) {
            validateSachDangBan(chiTiet.getSach());
            validateTonKho(chiTiet.getSach(), request.getSoLuong(), 0);
        }

        chiTiet.setSoLuong(request.getSoLuong());
        chiTietGioHangRepository.save(chiTiet);

        GioHang gioHang = chiTiet.getGioHang();
        capNhatThoiGian(gioHang);

        return buildCartResponse(gioHang);
    }

    @Transactional
    public CartResponse removeItem(Integer maND, Integer maCTGioHang) {

        ChiTietGioHang chiTiet = timDongTrongGioCuaToi(maCTGioHang, maND);

        GioHang gioHang = chiTiet.getGioHang();

        chiTietGioHangRepository.delete(chiTiet);
        chiTietGioHangRepository.flush();

        capNhatThoiGian(gioHang);

        return buildCartResponse(gioHang);
    }

    @Transactional
    public CartResponse clearCart(Integer maND) {

        GioHang gioHang = gioHangRepository.findByMaND(maND).orElse(null);

        if (gioHang == null) {
            return emptyCartResponse();
        }

        chiTietGioHangRepository.deleteAllByGioHang(gioHang.getMaGioHang());
        capNhatThoiGian(gioHang);

        return buildCartResponse(gioHang);
    }

    private GioHang getOrCreateGioHang(Integer maND) {

        return gioHangRepository.findByMaND(maND)
                .orElseGet(() -> {
                    NguoiDung nguoiDung = nguoiDungRepository.findById(maND)
                            .orElseThrow(() ->
                                    new BusinessException("Người dùng không tồn tại")
                            );

                    return gioHangRepository.save(
                            GioHang.builder()
                                    .nguoiDung(nguoiDung)
                                    .build()
                    );
                });
    }

    private ChiTietGioHang timDongTrongGioCuaToi(Integer maCTGioHang, Integer maND) {

        return chiTietGioHangRepository.findByIdAndOwner(maCTGioHang, maND)
                .orElseThrow(() ->
                        new BusinessException("Sản phẩm không có trong giỏ hàng")
                );
    }

    private boolean dangDuocBan(Sach sach) {
        return TrangThaiDuyet.DA_DUYET.name().equals(sach.getTrangThaiDuyet())
                && TrangThaiBan.DANG_BAN.name().equals(sach.getTrangThaiBan())
                && TrangThaiDuyet.DA_DUYET.name().equals(sach.getCuaHang().getTrangThaiDuyet());
    }

    private void validateSachDangBan(Sach sach) {
        if (!dangDuocBan(sach)) {
            throw new BusinessException("Sách hiện không còn được bán");
        }
    }

    private void validateSachCoTheMua(Sach sach, Integer maND) {

        validateSachDangBan(sach);

        if (sach.getGiaBanCu() == null) {
            throw new BusinessException("Sách chưa có giá bán");
        }

        if (sach.getSoLuongTon() == null || sach.getSoLuongTon() <= 0) {
            throw new BusinessException("Sách đã hết hàng");
        }

        NguoiDung chuShop = sach.getCuaHang().getChuShop();

        if (chuShop != null && maND.equals(chuShop.getMaND())) {
            throw new BusinessException(
                    "Bạn không thể mua sách của chính cửa hàng mình"
            );
        }
    }

    private void validateTonKho(Sach sach, int soLuongYeuCau, int soLuongDaCoTrongGio) {

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

    private void capNhatThoiGian(GioHang gioHang) {
        gioHang.setNgayCapNhat(LocalDateTime.now());
        gioHangRepository.save(gioHang);
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

    private CartResponse buildCartResponse(GioHang gioHang) {

        List<ChiTietGioHang> dongs =
                chiTietGioHangRepository.findAllByGioHang(gioHang.getMaGioHang());

        List<CartItemResponse> items = new ArrayList<>();
        int tongSoLuong = 0;
        BigDecimal tongTien = BigDecimal.ZERO;

        for (ChiTietGioHang ct : dongs) {

            Sach sach = ct.getSach();

            BigDecimal donGia =
                    sach.getGiaBanCu() == null ? BigDecimal.ZERO : sach.getGiaBanCu();

            BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(ct.getSoLuong()));

            int ton = sach.getSoLuongTon() == null ? 0 : sach.getSoLuongTon();

            String canhBao = null;

            if (!dangDuocBan(sach)) {
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
