package com.example.datnhathub.controller;

import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.ProductRepository;
import com.example.datnhathub.service.CartService;
import com.example.datnhathub.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {
    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

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

        List<Product> recommendProducts = productRepository.findAll();
        model.addAttribute("recommendProducts", recommendProducts);

        return "cart/index";
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
    public String checkoutPage(HttpSession session, Model model,
                               @RequestParam(required = false) List<Integer> selectedItems) {
        Integer userId = currentUserId(session);
        if (userId == null) return "redirect:/login";

        Cart cart = cartService.getCart(userId);
        if (cart.getDetails() == null || cart.getDetails().isEmpty()) {
            return "redirect:/cart";
        }

        List<CartDetail> checkoutItems = (selectedItems == null || selectedItems.isEmpty())
                ? cart.getDetails()
                : cart.getDetails().stream()
                .filter(d -> selectedItems.contains(d.getCartDetailId()))
                .toList();

        if (checkoutItems.isEmpty()) {
            return "redirect:/cart";
        }

        List<String> outOfStockMessages = new ArrayList<>();
        for (CartDetail cd : checkoutItems) {
            ProductDetail pd = cd.getProductDetail();
            int stock = pd.getStockQuantity() == null ? 0 : pd.getStockQuantity();
            if (cd.getQuantity() > stock) {
                outOfStockMessages.add(pd.getProduct().getProductName() + " (" + pd.getSize() + "/" + pd.getColor()
                        + ") chỉ còn " + stock + " sản phẩm trong kho.");
            }
        }

        BigDecimal total = checkoutItems.stream()
                .map(d -> d.getProductDetail().getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("cartDetails", checkoutItems);
        model.addAttribute("selectedItems", checkoutItems.stream().map(CartDetail::getCartDetailId).toList());
        model.addAttribute("total", total);
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
                             @RequestParam(required = false) List<Integer> selectedItems,
                             HttpSession session,
                             Model model,
                             RedirectAttributes ra) {

        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        String fullAddress = houseAddress.trim() + ", " + district.trim() + ", " + city.trim();

        try {
            Orders order = orderService.placeOrder(userId, fullAddress, city, paymentMethod, voucherCode, selectedItems);

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

    @GetMapping("/cart/recommend")
    public String viewAllRecommendations(HttpSession session, Model model) {
        Integer productId = (Integer) session.getAttribute("productid");

        List<Product> categoryProducts = new ArrayList<>();
        List<Product> brandProducts = new ArrayList<>();
        List<Product> latestProducts = productRepository.findByStatusTrueOrderByProductIdDesc();

        if (productId != null) {
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                if (product.getCategory() != null) {
                    categoryProducts = productRepository.findByCategory_CategoryIdAndProductIdNotAndStatusTrue(
                            product.getCategory().getCategoryId(), productId, org.springframework.data.domain.Pageable.unpaged()
                    );
                }
                if (product.getBrand() != null) {
                    brandProducts = productRepository.findByBrand_BrandIdAndProductIdNotAndStatusTrue(
                            product.getBrand().getBrandId(), productId, org.springframework.data.domain.Pageable.unpaged()
                    );
                }
            }
        }

        latestProducts.removeIf(p -> productId != null && p.getProductId().equals(productId));

        model.addAttribute("categoryProducts", categoryProducts);
        model.addAttribute("brandProducts", brandProducts);
        model.addAttribute("latestProducts", latestProducts);

        // Bổ sung thêm biến này để dự phòng trường hợp file HTML cũ đang gọi
        model.addAttribute("allRecommendProducts", latestProducts);

        Boolean loggedIn = session.getAttribute("userId") != null;
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("roleName", session.getAttribute("roleName"));

        return "cart/recommendations";
    }

    // Cập nhật biến thể sản phẩm trong giỏ hàng
    @PostMapping("/cart/update-variation")
    public String updateCartVariation(@RequestParam Integer cartDetailId,
                                      @RequestParam Integer productDetailId,
                                      RedirectAttributes ra) {
        try {
            cartService.updateVariation(cartDetailId, productDetailId);
            ra.addFlashAttribute("success", "Đã cập nhật biến thể thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }
}