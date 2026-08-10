package com.example.datnhathub.controller;

import com.example.datnhathub.dto.ProductDto;
import com.example.datnhathub.dto.ReviewDTO;
import com.example.datnhathub.entity.*;
import com.example.datnhathub.repository.*;
import com.example.datnhathub.service.ProductService;
import com.example.datnhathub.service.WishlistService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private WishlistService wishlistService;

    @GetMapping("/products")
    public String productList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        Page<ProductDto> productPage = productService.getProducts(
                keyword, categoryId, brandId, minPrice, maxPrice, sort, page
        );

        List<Category> categories = productService.getAllCategories();
        List<Brand> brands = productService.getAllBrands();

        Category selectedCategory = (categoryId != null)
                ? categories.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                .findFirst()
                .orElse(null)
                : null;

        Brand selectedBrand = (brandId != null)
                ? brands.stream()
                .filter(b -> b.getBrandId().equals(brandId))
                .findFirst()
                .orElse(null)
                : null;

        model.addAttribute("products",          productPage.getContent());
        model.addAttribute("totalPages",        productPage.getTotalPages());
        model.addAttribute("currentPage",       page);
        model.addAttribute("totalProducts",     productPage.getTotalElements());
        model.addAttribute("categories",        categories);
        model.addAttribute("selectedCategory",  selectedCategory);
        model.addAttribute("brands",            brands);
        model.addAttribute("selectedBrand",     selectedBrand);

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
    public String productDetail(@PathVariable Integer id,@RequestParam(defaultValue = "0") Integer pid, HttpSession session, Model model, HttpServletRequest req) {

        Product product = productService.getProductById(id);

        ArrayList<Integer> dspdid = reviewRepository.finddspdid(id);
        Integer pd = pid;
        if(pid == 0){
            pd = dspdid.get(0);
        }

        List<ReviewDTO> dsr = reviewRepository.findcomment(pd);

        List<ProductDetail> pdid = productDetailRepository.findByProductProductId(id);

        // ===== LỊCH SỬ XEM SẢN PHẨM =====
        List<Product> viewedProducts =
                (List<Product>) session.getAttribute("viewedProducts");

        if (viewedProducts == null) {
            viewedProducts = new ArrayList<>();
        }

// Nếu đã có thì xóa để đưa lên đầu
        viewedProducts.removeIf(p -> p.getProductId().equals(product.getProductId()));
// Thêm sản phẩm vừa xem lên đầu danh sách
        viewedProducts.add(0, product);
// Chỉ lưu tối đa 8 sản phẩm
        if (viewedProducts.size() > 8) {
            viewedProducts = viewedProducts.subList(0, 8);
        }

        session.setAttribute("viewedProducts", viewedProducts);

// Gửi sang HTML
        model.addAttribute("viewedProducts", viewedProducts);

        Map<Integer, String> viewedImageMap = new HashMap<>();
        for (Product p : viewedProducts) {
            viewedImageMap.put(p.getProductId(), productService.getDefaultImageUrl(p));
        }
        model.addAttribute("viewedImageMap", viewedImageMap);

        List<ReviewDTO> dsrx = reviewRepository.findcomment(product.getProductId());

        var details = product.getDetails();
        var selectedDetail = (details != null && !details.isEmpty()) ? details.get(0) : null;

        String iduser = "";
        for (Cookie c : req.getCookies()) {
            if(c.getName().equals("userId")){
                iduser = String.valueOf(c.getValue());
            }
        }
        System.out.println("UserID : "+iduser);

        Integer userID = (Integer) session.getAttribute("userId");

        Reviews r = new Reviews();
        r.setRating(0.0);
        r.setComment("");
        if (userID != null) {
            Reviews findr = reviewRepository.findByCustomerIDAndProductDetailID(userID, pd);
            if (findr != null) {
                r = findr;
            }
        }


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

        model.addAttribute("listc",dsrx);
        model.addAttribute("listpdi",pdid);
        model.addAttribute("cmt",r);

        Integer userId = (Integer) session.getAttribute("userId");
        model.addAttribute("inWishlist", wishlistService.isInWishlist(userId, product.getProductId()));

        return "products/detail";
    }

    @PostMapping("/comment")
    public String rep(Reviews review, HttpSession session, RedirectAttributes ra){
        Integer productid = Integer.parseInt(String.valueOf(session.getAttribute("productid")));
        Integer productDetailId = Integer.parseInt(String.valueOf(session.getAttribute("selectedDetail")));

        Integer userID = (Integer) session.getAttribute("userId");
        if (userID == null) {
            return "redirect:/login";
        }

        ProductDetail pd = productDetailRepository.findById(productDetailId).get();
        Customer customer = customerRepository.findByUserUserID(userID).get();
        review.setProductDetailID(pd);
        review.setCustomerID(customer);

        try {
            reviewRepository.save(review);
            ra.addFlashAttribute("Csuccess", "Cảm ơn bạn đã đánh giá!");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("Cerror", "Mỗi sản phẩm chỉ được đánh giá 1 lần!");
        }

        return "redirect:/products/" + productid;
    }
}