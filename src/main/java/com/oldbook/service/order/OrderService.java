package com.oldbook.service.order;

import com.oldbook.constant.catalog.ApprovalStatus;
import com.oldbook.constant.catalog.SaleStatus;
import com.oldbook.constant.order.OrderStatus;
import com.oldbook.constant.shipping.CarrierStatus;
import com.oldbook.dto.order.CreateOrderRequest;
import com.oldbook.dto.order.OrderResponse;
import com.oldbook.dto.order.StoreOrderOptionRequest;
import com.oldbook.dto.order.StoreOrderResponse;
import com.oldbook.entity.cart.Cart;
import com.oldbook.entity.cart.CartItem;
import com.oldbook.entity.catalog.Book;
import com.oldbook.entity.catalog.Store;
import com.oldbook.entity.identity.Address;
import com.oldbook.entity.identity.User;
import com.oldbook.entity.order.Order;
import com.oldbook.entity.order.OrderItem;
import com.oldbook.entity.order.StoreOrder;
import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.exception.common.BusinessException;
import com.oldbook.repository.cart.CartItemRepository;
import com.oldbook.repository.cart.CartRepository;
import com.oldbook.repository.catalog.BookRepository;
import com.oldbook.repository.identity.AddressRepository;
import com.oldbook.repository.identity.UserRepository;
import com.oldbook.repository.order.OrderItemRepository;
import com.oldbook.repository.order.OrderRepository;
import com.oldbook.repository.order.StoreOrderRepository;
import com.oldbook.repository.shipping.ShippingCarrierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String PAYMENT_METHOD_COD = "COD";
    private static final String PAYMENT_STATUS_PENDING = "CHO_THANH_TOAN";
    private static final String NEW_ORDER_STATUS = OrderStatus.CHO_XU_LY.name();

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ShippingCarrierRepository shippingCarrierRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public OrderResponse createOrder(Integer maND, CreateOrderRequest request) {
        if (!PAYMENT_METHOD_COD.equalsIgnoreCase(request.getPhuongThucTT().trim())) {
            throw new BusinessException("Phương thức thanh toán hiện hỗ trợ: COD");
        }

        User nguoiMua = userRepository.findById(maND)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));

        Address diaChi = addressRepository
                .findByMaDiaChiAndNguoiDung_MaND(request.getMaDiaChi(), maND)
                .orElseThrow(() -> new BusinessException(
                        "Địa chỉ giao hàng không tồn tại hoặc không thuộc tài khoản của bạn"
                ));

        Cart cart = cartRepository.findByMaND(maND)
                .orElseThrow(() -> new BusinessException("Giỏ hàng đang trống"));

        // Khóa giỏ hàng để không cho hai request checkout đồng thời dùng cùng một giỏ.
        cart = cartRepository.findByIdForUpdate(cart.getMaGioHang())
                .orElseThrow(() -> new BusinessException("Giỏ hàng không tồn tại"));

        List<CartItem> allCartItems =
                cartItemRepository.findAllByGioHang(cart.getMaGioHang());

        if (allCartItems.isEmpty()) {
            throw new BusinessException("Giỏ hàng đang trống");
        }

        // Chỉ đặt các dòng giỏ hàng mà khách đã tích chọn.
        Set<Integer> selectedIds = new HashSet<>(request.getMaCTGioHangs());

        List<CartItem> cartItems = allCartItems.stream()
                .filter(ct -> selectedIds.contains(ct.getMaCTGioHang()))
                .toList();

        if (cartItems.isEmpty() || cartItems.size() != selectedIds.size()) {
            throw new BusinessException(
                    "Sản phẩm được chọn không còn trong giỏ hàng. Vui lòng quay lại giỏ hàng và chọn lại"
            );
        }

        List<CheckoutLine> lines = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Integer maSach = cartItem.getSach().getMaSach();

            Book sach = bookRepository.findByIdForUpdate(maSach)
                    .orElseThrow(() -> new BusinessException(
                            "Sách không tồn tại: " + maSach
                    ));

            validateCanOrder(sach, maND, cartItem.getSoLuong());

            BigDecimal donGia = sach.getGiaBanCu();
            BigDecimal thanhTien = donGia.multiply(
                    BigDecimal.valueOf(cartItem.getSoLuong())
            );

            lines.add(new CheckoutLine(cartItem, sach, donGia, thanhTien));
        }

        // Gom sách theo cửa hàng: mỗi cửa hàng là một đơn hàng con riêng.
        Map<Integer, List<CheckoutLine>> linesByStore = lines.stream()
                .collect(Collectors.groupingBy(
                        l -> l.sach().getCuaHang().getMaCH(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // Mỗi cửa hàng phải có đúng một lựa chọn (đơn vị vận chuyển + ghi chú riêng).
        Map<Integer, StoreOrderOptionRequest> optionsByStore =
                indexStoreOptions(request.getStoreOrders(), linesByStore);

        Map<Integer, ShippingCarrier> carrierCache = new HashMap<>();
        List<StoreDraft> drafts = new ArrayList<>();

        for (Map.Entry<Integer, List<CheckoutLine>> entry : linesByStore.entrySet()) {
            List<CheckoutLine> storeLines = entry.getValue();
            StoreOrderOptionRequest option = optionsByStore.get(entry.getKey());

            ShippingCarrier carrier = carrierCache.computeIfAbsent(
                    option.getMaDVVC(), this::loadActiveCarrier
            );

            BigDecimal tienHangCH = storeLines.stream()
                    .map(CheckoutLine::thanhTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal phiShipCH = Optional.ofNullable(carrier.getPhiVanChuyen())
                    .orElse(BigDecimal.ZERO);

            drafts.add(new StoreDraft(
                    storeLines.get(0).sach().getCuaHang(),
                    storeLines,
                    carrier,
                    normalizeNote(option.getGhiChu()),
                    tienHangCH,
                    phiShipCH
            ));
        }

        BigDecimal tongTienHang = drafts.stream()
                .map(StoreDraft::tienHang)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tongPhiShip = drafts.stream()
                .map(StoreDraft::phiShip)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tongGiamGia = BigDecimal.ZERO;
        BigDecimal tongThanhToan = tongTienHang
                .add(tongPhiShip)
                .subtract(tongGiamGia);

        Order donHang = Order.builder()
                .nguoiMua(nguoiMua)
                .giamGiaSan(BigDecimal.ZERO)
                .tongTienHang(tongTienHang)
                .tongPhiShip(tongPhiShip)
                .tongGiamGia(tongGiamGia)
                .tongThanhToan(tongThanhToan)
                .phuongThucTT(PAYMENT_METHOD_COD)
                .trangThaiThanhToan(PAYMENT_STATUS_PENDING)
                .diaChiGiao(diaChi)
                .tenNguoiNhan(diaChi.getTenNguoiNhan())
                .soDienThoaiNhan(diaChi.getSoDienThoai())
                .tinhThanhGiao(diaChi.getTinhThanh())
                .quanHuyenGiao(diaChi.getQuanHuyen())
                .phuongXaGiao(diaChi.getPhuongXa())
                .diaChiGiaoChiTiet(diaChi.getDiaChiChiTiet())
                .build();

        donHang = orderRepository.save(donHang);

        for (StoreDraft draft : drafts) {
            StoreOrder dhch = StoreOrder.builder()
                    .donHang(donHang)
                    .cuaHang(draft.cuaHang())
                    .donViVanChuyen(draft.carrier())
                    .giamGiaShop(BigDecimal.ZERO)
                    .giamGiaSanPhanBo(BigDecimal.ZERO)
                    .tienHangCH(draft.tienHang())
                    .phiShipCH(draft.phiShip())
                    .commissionTien(BigDecimal.ZERO)
                    .tongTienShopOrder(draft.tienHang().add(draft.phiShip()))
                    .trangThaiDonHang(NEW_ORDER_STATUS)
                    .trangThaiThanhToanShop(PAYMENT_STATUS_PENDING)
                    .tienHoanTra(BigDecimal.ZERO)
                    .ghiChu(draft.ghiChu())
                    .build();

            dhch = storeOrderRepository.save(dhch);

            for (CheckoutLine line : draft.lines()) {
                OrderItem chiTiet = OrderItem.builder()
                        .donHangCuaHang(dhch)
                        .sach(line.sach())
                        .soLuong(line.cartItem().getSoLuong())
                        .donGia(line.donGia())
                        .thanhTien(line.thanhTien())
                        .tyLeChietKhau(BigDecimal.ZERO)
                        .tienChietKhau(BigDecimal.ZERO)
                        .build();

                orderItemRepository.save(chiTiet);

                // Đã khóa bản ghi Book trong transaction nên không xảy ra trừ tồn kiểu lost-update.
                line.sach().setSoLuongTon(
                        line.sach().getSoLuongTon() - line.cartItem().getSoLuong()
                );
                bookRepository.save(line.sach());
            }
        }

        // Chỉ xóa các dòng đã đặt; sản phẩm không được chọn vẫn còn trong giỏ.
        cartItemRepository.deleteAll(cartItems);
        cartItemRepository.flush();

        return buildOrderResponse(donHang);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Integer maND) {
        return orderRepository.findAllByNguoiMua(maND).stream()
                .map(this::buildOrderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Integer maND, Integer maDH) {
        Order donHang = orderRepository.findById(maDH)
                .orElseThrow(() -> new BusinessException("Đơn hàng không tồn tại"));

        if (donHang.getNguoiMua() == null
                || !maND.equals(donHang.getNguoiMua().getMaND())) {
            throw new BusinessException("Bạn không có quyền xem đơn hàng này");
        }

        return buildOrderResponse(donHang);
    }

    private Map<Integer, StoreOrderOptionRequest> indexStoreOptions(
            List<StoreOrderOptionRequest> options,
            Map<Integer, List<CheckoutLine>> linesByStore
    ) {
        if (options == null || options.isEmpty()) {
            throw new BusinessException(
                    "Vui lòng chọn đơn vị vận chuyển cho từng cửa hàng"
            );
        }

        Map<Integer, StoreOrderOptionRequest> byStore = new LinkedHashMap<>();
        for (StoreOrderOptionRequest option : options) {
            if (!linesByStore.containsKey(option.getMaCH())) {
                throw new BusinessException(
                        "Cửa hàng #" + option.getMaCH() + " không có trong giỏ hàng của bạn"
                );
            }
            if (byStore.put(option.getMaCH(), option) != null) {
                throw new BusinessException(
                        "Mỗi cửa hàng chỉ được chọn một đơn vị vận chuyển"
                );
            }
        }

        for (Map.Entry<Integer, List<CheckoutLine>> entry : linesByStore.entrySet()) {
            if (!byStore.containsKey(entry.getKey())) {
                Store store = entry.getValue().get(0).sach().getCuaHang();
                throw new BusinessException(
                        "Vui lòng chọn đơn vị vận chuyển cho cửa hàng \""
                                + store.getTenCuaHang() + "\""
                );
            }
        }

        return byStore;
    }

    private ShippingCarrier loadActiveCarrier(Integer maDVVC) {
        return shippingCarrierRepository
                .findByMaDVVCAndTrangThai(maDVVC, CarrierStatus.HOAT_DONG.name())
                .orElseThrow(() -> new BusinessException(
                        "Đơn vị vận chuyển không tồn tại hoặc đã ngừng hoạt động"
                ));
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateCanOrder(Book sach, Integer maND, Integer soLuong) {
        if (soLuong == null || soLuong <= 0) {
            throw new BusinessException("Số lượng đặt hàng không hợp lệ");
        }

        if (!ApprovalStatus.DA_DUYET.name().equals(sach.getTrangThaiDuyet())
                || !SaleStatus.DANG_BAN.name().equals(sach.getTrangThaiBan())
                || sach.getCuaHang() == null
                || !ApprovalStatus.DA_DUYET.name().equals(
                        sach.getCuaHang().getTrangThaiDuyet())) {
            throw new BusinessException(
                    "Sách \"" + sach.getTenSach() + "\" hiện không còn được bán"
            );
        }

        if (sach.getGiaBanCu() == null
                || sach.getGiaBanCu().signum() <= 0) {
            throw new BusinessException(
                    "Sách \"" + sach.getTenSach() + "\" chưa có giá bán hợp lệ"
            );
        }

        int ton = Optional.ofNullable(sach.getSoLuongTon()).orElse(0);
        if (ton < soLuong) {
            throw new BusinessException(
                    "Sách \"" + sach.getTenSach()
                            + "\" không đủ tồn kho. Còn " + ton + " cuốn"
            );
        }

        User chuShop = sach.getCuaHang().getChuShop();
        if (chuShop != null && maND.equals(chuShop.getMaND())) {
            throw new BusinessException(
                    "Bạn không thể mua sách của chính cửa hàng mình"
            );
        }
    }

    private OrderResponse buildOrderResponse(Order donHang) {
        List<StoreOrder> stores =
                storeOrderRepository.findAllByDonHang_MaDHOrderByMaDHCHAsc(
                        donHang.getMaDH()
                );

        List<StoreOrderResponse> storeResponses =
                orderMapper.toStoreOrderResponses(stores);

        return OrderResponse.builder()
                .maDH(donHang.getMaDH())
                .maNDNguoiMua(donHang.getNguoiMua().getMaND())
                .tongTienHang(donHang.getTongTienHang())
                .tongPhiShip(donHang.getTongPhiShip())
                .tongGiamGia(donHang.getTongGiamGia())
                .tongThanhToan(donHang.getTongThanhToan())
                .phuongThucTT(donHang.getPhuongThucTT())
                .trangThaiThanhToan(donHang.getTrangThaiThanhToan())
                .maDiaChi(donHang.getDiaChiGiao() == null
                        ? null : donHang.getDiaChiGiao().getMaDiaChi())
                .tenNguoiNhan(donHang.getTenNguoiNhan())
                .soDienThoaiNhan(donHang.getSoDienThoaiNhan())
                .tinhThanhGiao(donHang.getTinhThanhGiao())
                .quanHuyenGiao(donHang.getQuanHuyenGiao())
                .phuongXaGiao(donHang.getPhuongXaGiao())
                .diaChiGiaoChiTiet(donHang.getDiaChiGiaoChiTiet())
                .ngayTao(donHang.getNgayTao())
                .storeOrders(storeResponses)
                .build();
    }

    private record StoreDraft(
            Store cuaHang,
            List<CheckoutLine> lines,
            ShippingCarrier carrier,
            String ghiChu,
            BigDecimal tienHang,
            BigDecimal phiShip
    ) {}

    private record CheckoutLine(
            CartItem cartItem,
            Book sach,
            BigDecimal donGia,
            BigDecimal thanhTien
    ) {}
}
