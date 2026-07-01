package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Cart;
import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.service.CartService;
import com.example.datnhathub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    // Helper: lấy userId từ session, nếu chưa đăng nhập -> null
    private Integer currentUserId(HttpSession session) {
        return (Integer) session.getAttribute("userId");
    }

    // UC16 — Thêm sản phẩm vào giỏ hàng
    @PostMapping("/products/{id}/add-to-cart")
    public String addToCart(@PathVariable("id") Integer productId,
                            @RequestParam Integer productDetailId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            HttpSession session,
                            RedirectAttributes ra) {

        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            cartService.addToCart(userId, productDetailId, quantity);
            ra.addFlashAttribute("success", "Đã thêm vào giỏ hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/products/" + productId;
    }

    // UC17 — Xem giỏ hàng
    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        Cart cart = cartService.getCart(userId);
        model.addAttribute("cart", cart);
        model.addAttribute("total", cartService.calculateTotal(cart));
        model.addAttribute("loggedIn", true);
        model.addAttribute("roleName", session.getAttribute("roleName"));

        return "cart/index"; // templates/cart/index.html
    }

    // UC18 — Cập nhật số lượng trong giỏ
    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Integer cartDetailId,
                             @RequestParam Integer quantity,
                             RedirectAttributes ra) {
        try {
            cartService.updateQuantity(cartDetailId, quantity);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    // UC19 — Xóa sản phẩm khỏi giỏ
    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Integer cartDetailId, RedirectAttributes ra) {
        cartService.removeItem(cartDetailId);
        ra.addFlashAttribute("success", "Đã xóa sản phẩm khỏi giỏ.");
        return "redirect:/cart";
    }

    // UC20 — Trang Checkout (xem trước khi đặt)
    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        Cart cart = cartService.getCart(userId);
        if (cart.getDetails() == null || cart.getDetails().isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("total", cartService.calculateTotal(cart));
        return "checkout/checkout"; // templates/cart/checkout.html
    }

    // UC20 — Đặt hàng từ giỏ hàng
    @PostMapping("/checkout")
    public String placeOrder(@RequestParam String shippingAddress,
                             @RequestParam String paymentMethod,
                             @RequestParam(required = false) String voucherCode,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {

        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            Orders order = orderService.placeOrder(userId, shippingAddress, paymentMethod, voucherCode);

            if ("Chuyển khoản".equals(paymentMethod)
                    && order.getStatus().equals("Chờ thanh toán")) {
                return "redirect:/payment/qr/" + order.getOrderId();
            }

            ra.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: #" + order.getOrderId());
            return "redirect:/orders/" + order.getOrderId();
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("total", cartService.calculateTotal(cartService.getCart(userId)));
            return "checkout/index";
        }
    }
}
