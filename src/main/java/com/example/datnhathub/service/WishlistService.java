package com.example.datnhathub.service;

import com.example.datnhathub.entity.Customer;
import com.example.datnhathub.entity.Product;
import com.example.datnhathub.entity.Wishlist;
import com.example.datnhathub.entity.WishlistDetail;
import com.example.datnhathub.repository.CustomerRepository;
import com.example.datnhathub.repository.ProductRepository;
import com.example.datnhathub.repository.WishlistDetailRepository;
import com.example.datnhathub.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class WishlistService {

    @Autowired private WishlistRepository wishlistRepository;
    @Autowired private WishlistDetailRepository wishlistDetailRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;

    // Lấy Customer hiện tại từ userId trong session (giống CartService)
    public Customer getCustomerByUserId(Integer userId) {
        return customerRepository.findByUserUserID(userId)
                .orElseThrow(() -> new RuntimeException("Tài khoản này không phải khách hàng (Customer)."));
    }

    // Lấy wishlist hiện tại, tự tạo mới nếu Customer chưa có
    public Wishlist getOrCreateWishlist(Customer customer) {
        return wishlistRepository.findByCustomerCustomerId(customer.getCustomerId())
                .orElseGet(() -> {
                    Wishlist wishlist = new Wishlist();
                    wishlist.setCustomer(customer);
                    return wishlistRepository.save(wishlist);
                });
    }

    public Wishlist getWishlist(Integer userId) {
        Customer customer = getCustomerByUserId(userId);
        return getOrCreateWishlist(customer);
    }

    // Bấm tim: nếu đã có trong wishlist -> xóa, nếu chưa có -> thêm. Trả về true nếu vừa được thêm.
    @Transactional
    public boolean toggleWishlist(Integer userId, Integer productId) {
        Customer customer = getCustomerByUserId(userId);
        Wishlist wishlist = getOrCreateWishlist(customer);

        var existing = wishlistDetailRepository
                .findByWishlistWishlistIdAndProductProductId(wishlist.getWishlistId(), productId);

        if (existing.isPresent()) {
            wishlistDetailRepository.delete(existing.get());
            return false; // vừa bỏ khỏi wishlist
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId));

        WishlistDetail detail = new WishlistDetail();
        detail.setWishlist(wishlist);
        detail.setProduct(product);
        detail.setAddedDate(LocalDateTime.now());
        wishlistDetailRepository.save(detail);
        return true; // vừa thêm vào wishlist
    }

    // Xóa 1 sản phẩm khỏi wishlist (dùng trong trang wishlist)
    @Transactional
    public void removeFromWishlist(Integer userId, Integer productId) {
        Wishlist wishlist = getWishlist(userId);
        wishlistDetailRepository.deleteByWishlistIdAndProductId(wishlist.getWishlistId(), productId);
    }

    // Dùng để tô đậm icon trái tim trên trang danh sách/chi tiết sản phẩm
    public boolean isInWishlist(Integer userId, Integer productId) {
        if (userId == null) return false;
        try {
            Wishlist wishlist = getWishlist(userId);
            return wishlistDetailRepository
                    .findByWishlistWishlistIdAndProductProductId(wishlist.getWishlistId(), productId)
                    .isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    // Số lượng sản phẩm trong wishlist, dùng hiển thị badge trên nav
    public int getWishlistItemCount(Integer userId) {
        if (userId == null) return 0;
        try {
            Wishlist wishlist = getWishlist(userId);
            return wishlist.getDetails() == null ? 0 : wishlist.getDetails().size();
        } catch (Exception e) {
            return 0;
        }
    }
}
