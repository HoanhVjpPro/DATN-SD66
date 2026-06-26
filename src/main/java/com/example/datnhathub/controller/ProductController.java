package com.example.datnhathub.controller;

import com.example.datnhathub.dto.ProductDto;
import com.example.datnhathub.entity.Category;
import com.example.datnhathub.entity.Product;
import com.example.datnhathub.repository.ProductRepository;
import com.example.datnhathub.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProductController {
    @Autowired
    private ProductService productService;
    // FIX: đã xóa @Autowired ProductRepository - controller không nên gọi trực tiếp Repository,
    // luôn đi qua Service để giữ đúng kiến trúc layered (Controller -> Service -> Repository)

    @GetMapping("/products")
    public String productList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        Page<ProductDto> productPage = productService.getProducts(
                keyword, categoryId, minPrice, maxPrice, sort, page
        );

        List<Category> categories = productService.getAllCategories();

        Category selectedCategory = (categoryId != null)
                ? categories.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                .findFirst()
                .orElse(null)
                : null;

        model.addAttribute("products",          productPage.getContent());
        model.addAttribute("totalPages",        productPage.getTotalPages());
        model.addAttribute("currentPage",       page);
        model.addAttribute("totalProducts",     productPage.getTotalElements());
        model.addAttribute("categories",        categories);
        model.addAttribute("selectedCategory",  selectedCategory);

        model.addAttribute("keyword",   keyword);
        model.addAttribute("minPrice",  minPrice);
        model.addAttribute("maxPrice",  maxPrice);
        model.addAttribute("sort",      sort);

        Boolean loggedIn = session.getAttribute("userId") != null;
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("roleName", session.getAttribute("roleName"));

        return "products/product-list";
    }

    // ════════════════════════════════════════
    // UC07 — Chi tiết sản phẩm
    // GET /products/{id}
    // ════════════════════════════════════════
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Integer id, HttpSession session, Model model) {

        Product product = productService.getProductById(id);

        var details = product.getDetails();
        var selectedDetail = (details != null && !details.isEmpty()) ? details.get(0) : null;

        String imageUrl = productService.getDefaultImageUrl(product);

        model.addAttribute("product",        product);
        model.addAttribute("details",        details);
        model.addAttribute("selectedDetail", selectedDetail);
        model.addAttribute("imageUrl",       imageUrl);

        Boolean loggedIn = session.getAttribute("userId") != null;
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("roleName", session.getAttribute("roleName"));

        return "products/detail";
    }
}
