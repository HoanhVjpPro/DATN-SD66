package com.example.datnhathub.service;

import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartDetailRepository cartDetailRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductDetailRepository productDetailRepository;

    // ════════════════════════════════════════
    // Helper: lấy Customer hiện tại từ userId trong session
    // ════════════════════════════════════════
    public Customer getCustomerByUserId(Integer userId) {
        return customerRepository.findByUserUserID(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản này không phải khách hàng (Customer)."));
    }

    // ════════════════════════════════════════
    // Helper: lấy giỏ hàng hiện tại, tự tạo mới nếu Customer chưa có giỏ
    // ════════════════════════════════════════
    public Cart getOrCreateCart(Customer customer) {
        return cartRepository.findByCustomerCustomerId(customer.getCustomerId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });
    }

    // ════════════════════════════════════════
    // UC16 — Thêm sản phẩm vào giỏ hàng
    // FIX: validate stockQuantity trước khi thêm / cộng dồn
    // ════════════════════════════════════════
    public void addToCart(Integer userId, Integer productDetailId, int quantity) {
        if (quantity < 1) quantity = 1;

        Customer customer = getCustomerByUserId(userId);
        Cart cart = getOrCreateCart(customer);

        ProductDetail productDetail = productDetailRepository.findById(productDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm ID: " + productDetailId));

        var existing = cartDetailRepository
                .findByCartCartIdAndProductDetailProductDetailId(cart.getCartId(), productDetailId);

        int currentQtyInCart = existing.map(CartDetail::getQuantity).orElse(0);
        int newTotalQty = currentQtyInCart + quantity;

        // FIX: validate tồn kho - không cho thêm vượt số lượng còn lại
        if (productDetail.getStockQuantity() == null || newTotalQty > productDetail.getStockQuantity()) {
            throw new RuntimeException(
                    "Chỉ còn " + (productDetail.getStockQuantity() == null ? 0 : productDetail.getStockQuantity())
                            + " sản phẩm trong kho (bạn đang có " + currentQtyInCart + " trong giỏ)."
            );
        }

        if (existing.isPresent()) {
            CartDetail detail = existing.get();
            detail.setQuantity(newTotalQty);
            cartDetailRepository.save(detail);
        } else {
            CartDetail detail = new CartDetail();
            detail.setCart(cart);
            detail.setProductDetail(productDetail);
            detail.setQuantity(quantity);
            cartDetailRepository.save(detail);
        }
    }

    // ════════════════════════════════════════
    // UC17 — Xem giỏ hàng
    // ════════════════════════════════════════
    public Cart getCart(Integer userId) {
        Customer customer = getCustomerByUserId(userId);
        return getOrCreateCart(customer);
    }

    public BigDecimal calculateTotal(Cart cart) {
        if (cart.getDetails() == null || cart.getDetails().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return cart.getDetails().stream()
                .map(d -> d.getProductDetail().getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ════════════════════════════════════════
    // UC18 — Cập nhật số lượng trong giỏ
    // FIX: validate không vượt tồn kho
    // ════════════════════════════════════════
    public void updateQuantity(Integer cartDetailId, int quantity) {
        if (quantity < 1) {
            // Số lượng <= 0 -> coi như xóa luôn dòng đó khỏi giỏ
            cartDetailRepository.deleteById(cartDetailId);
            return;
        }

        CartDetail detail = cartDetailRepository.findById(cartDetailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ."));

        Integer stock = detail.getProductDetail().getStockQuantity();
        if (stock == null || quantity > stock) {
            throw new RuntimeException("Chỉ còn " + (stock == null ? 0 : stock) + " sản phẩm trong kho.");
        }

        detail.setQuantity(quantity);
        cartDetailRepository.save(detail);
    }

    // ════════════════════════════════════════
    // UC19 — Xóa sản phẩm khỏi giỏ
    // ════════════════════════════════════════
    public void removeItem(Integer cartDetailId) {
        cartDetailRepository.deleteById(cartDetailId);
    }

    public int getCartItemCount(Integer userId) {
        if (userId == null) return 0;
        try {
            Cart cart = getCart(userId);
            if (cart.getDetails() == null) return 0;
            return cart.getDetails().stream().mapToInt(CartDetail::getQuantity).sum();
        } catch (Exception e) {
            return 0;
        }
    }
}