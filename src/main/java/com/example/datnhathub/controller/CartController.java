package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Cart;
import com.example.datnhathub.entity.CartDetail;
import com.example.datnhathub.entity.Orders;
import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.service.CartService;
import com.example.datnhathub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

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

        // Kiểm tra tồn kho trước khi cho phép đặt hàng
        List<String> outOfStockMessages = new ArrayList<>();
        for (CartDetail cd : cart.getDetails()) {
            ProductDetail pd = cd.getProductDetail();
            int stock = pd.getStockQuantity() == null ? 0 : pd.getStockQuantity();
            if (cd.getQuantity() > stock) {
                outOfStockMessages.add(
                        pd.getProduct().getProductName() + " (" + pd.getSize() + "/" + pd.getColor() + ") chỉ còn "
                                + stock + " sản phẩm trong kho, giỏ hàng đang đặt " + cd.getQuantity() + "."
                );
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("total", cartService.calculateTotal(cart));
        model.addAttribute("outOfStockMessages", outOfStockMessages);
        return "checkout/checkout";
    }

    // UC20 — Đặt hàng từ giỏ hàng
    // POST /checkout — Đặt hàng từ giỏ hàng
    @PostMapping("/checkout")
    public String placeOrder(@RequestParam String houseAddress,
                             @RequestParam String district,
                             @RequestParam String city,
                             @RequestParam String paymentMethod,
                             @RequestParam(required = false) String voucherCode,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {

        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        String fullAddress = houseAddress.trim() + ", " + district.trim() + ", " + city.trim();

        try {
            Orders order = orderService.placeOrder(userId, fullAddress, city, paymentMethod, voucherCode);

            if ("Chuyển khoản".equals(paymentMethod)
                    && order.getStatus().equals("Chờ thanh toán")) {
                return "redirect:/payment/qr/" + order.getOrderId();
            }

            ra.addFlashAttribute("success", "Đặt hàng thành công! Mã đơn: #" + order.getOrderCode());
            return "redirect:/orders/" + order.getOrderId();
        } catch (Exception e) {
            e.printStackTrace();
            Cart cart = cartService.getCart(userId);
            model.addAttribute("cart", cart);
            model.addAttribute("total", cartService.calculateTotal(cart));
            model.addAttribute("error", e.getMessage());
            return "checkout/checkout";
        }
    }
}