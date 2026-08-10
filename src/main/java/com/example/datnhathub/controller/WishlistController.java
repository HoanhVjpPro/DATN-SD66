package com.example.datnhathub.controller;

import com.example.datnhathub.entity.Wishlist;
import com.example.datnhathub.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    private Integer currentUserId(HttpSession session) {
        return (Integer) session.getAttribute("userId");
    }

    // GET /wishlist — Xem danh sách yêu thích
    @GetMapping("/wishlist")
    public String viewWishlist(HttpSession session, Model model) {
        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        Wishlist wishlist = wishlistService.getWishlist(userId);
        model.addAttribute("wishlist", wishlist);
        model.addAttribute("loggedIn", true);
        model.addAttribute("roleName", session.getAttribute("roleName"));

        return "wishlist/index"; // templates/wishlist/index.html
    }

    // POST /products/{id}/wishlist/toggle — Thêm / bỏ khỏi wishlist
    // Dùng chung cho nút trái tim ở trang danh sách sản phẩm & trang chi tiết sản phẩm.
    // redirectTo cho phép quay lại đúng trang đang đứng (mặc định /products).
    @PostMapping("/products/{id}/wishlist/toggle")
    public String toggleWishlist(@PathVariable("id") Integer productId,
                                 @RequestParam(required = false, defaultValue = "/products") String redirectTo,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            boolean added = wishlistService.toggleWishlist(userId, productId);
            ra.addFlashAttribute("success",
                    added ? "Đã thêm vào danh sách yêu thích!" : "Đã bỏ khỏi danh sách yêu thích!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:" + redirectTo;
    }

    // POST /wishlist/remove — Xóa 1 sản phẩm khỏi wishlist (dùng trong chính trang wishlist)
    @PostMapping("/wishlist/remove")
    public String removeFromWishlist(@RequestParam Integer productId,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        Integer userId = currentUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        wishlistService.removeFromWishlist(userId, productId);
        ra.addFlashAttribute("success", "Đã xóa khỏi danh sách yêu thích!");
        return "redirect:/wishlist";
    }
}
