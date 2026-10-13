package com.oldbook.controller.shipping;

import com.oldbook.entity.shipping.ShippingCarrier;
import com.oldbook.service.shipping.ShippingCarrierResolver;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ShippingWebController {

    private final ShippingCarrierResolver donViResolver;

    /** Không có mã thì không biết là hãng nào; cũng không liệt kê các hãng để tránh lộ link. */
    @GetMapping("/shipping")
    public String shippingWithoutCode(Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("message", "Liên kết thiếu mã đơn vị vận chuyển. Vui lòng dùng đúng link được cấp, ví dụ /shipping/GHN.");
        return "shipping/not-found";
    }

    /** Trang của từng hãng: cùng giao diện, dữ liệu lọc theo maCode. Không cần đăng nhập. */
    @GetMapping("/shipping/{maCode}")
    public String shippingPage(@PathVariable String maCode, Model model, HttpServletResponse response) {
        Optional<ShippingCarrier> found = donViResolver.find(maCode);
        if (found.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute("message", "Không tìm thấy đơn vị vận chuyển với mã này.");
            return "shipping/not-found";
        }

        ShippingCarrier dvvc = found.get();
        if (!ShippingCarrierResolver.isActive(dvvc)) {
            model.addAttribute("message", "Đơn vị vận chuyển \"" + dvvc.getTenDonVi() + "\" đang ngừng hoạt động.");
            return "shipping/not-found";
        }

        model.addAttribute("maCode", dvvc.getMaCode());
        model.addAttribute("tenDonVi", dvvc.getTenDonVi());
        return "shipping/dashboard";
    }

    /** Trang quản lý đối tác vận chuyển; quyền được kiểm ở API (/api/management/**) và JS. */
    @GetMapping("/management/shipping-carriers")
    public String carrierManagementPage() {
        return "shipping/carriers";
    }
}
