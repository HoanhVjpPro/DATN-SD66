package com.example.datnhathub.controller;

import com.example.datnhathub.dto.ProductDto;
import com.example.datnhathub.dto.ReviewDTO;
import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.*;
import com.example.datnhathub.service.ProductService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class ProductController {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductDetailRepository productDetailRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ReviewRepository reviewRepository;

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

    // UC07 — Chi tiết sản phẩm
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Integer id, HttpSession session, Model model) {

        Product product = productService.getProductById(id);

        List<ReviewDTO> dsr = reviewRepository.findcomment(product.getProductId());

        var details = product.getDetails();
        var selectedDetail = (details != null && !details.isEmpty()) ? details.get(0) : null;

        String imageUrl = productService.getDefaultImageUrl(product); // FIX: gọi từ Service, không phải Repository

        session.setAttribute("productid",product.getProductId());
        session.setAttribute("selectedDetail",selectedDetail.getProductDetailId());

        model.addAttribute("product",        product);
        model.addAttribute("details",        details);
        model.addAttribute("selectedDetail", selectedDetail);
        model.addAttribute("imageUrl",       imageUrl);

        Boolean loggedIn = session.getAttribute("userId") != null;
        model.addAttribute("loggedIn", loggedIn);
        model.addAttribute("roleName", session.getAttribute("roleName"));

        model.addAttribute("listc",dsr);

        return "products/detail";
    }

    @PostMapping("/comment")
    public String rep(Reviews review, HttpSession session, HttpServletRequest req, RedirectAttributes ra){
        Integer productid = Integer.parseInt(String.valueOf(session.getAttribute("productid")));
        Integer productDetailId = Integer.parseInt(String.valueOf(session.getAttribute("selectedDetail")));

        String iduser = new String();
        for (Cookie c : req.getCookies()) {
            if(c.getName().equals("userId")){
                iduser = String.valueOf(c.getValue());
            }
        }
        Integer userID = Integer.parseInt(iduser);
        ProductDetail pd = productDetailRepository.findById(productDetailId).get();
        Customer customer = customerRepository.findByUserUserID(userID).get();
        review.setProductDetailID(pd);
        review.setCustomerID(customer);

        try {
            reviewRepository.save(review);
            ra.addFlashAttribute("Csuccess", "Cảm ơn bạn đã đánh giá!");
        }catch (DataIntegrityViolationException e){
            ra.addFlashAttribute("Cerror", "Mỗi sản phẩm chỉ được đánh giá 1 lần!");
        }

        return "redirect:/products/"+productid;
    }
}
