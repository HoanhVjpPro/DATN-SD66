package com.example.datnhathub.controller;

import com.example.datnhathub.entity.ProductDetail;
import com.example.datnhathub.entity.Reviews;
import com.example.datnhathub.repository.ProductDetailRepository;
import com.example.datnhathub.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminReviewController {
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ProductDetailRepository productDetailRepository;

    @GetMapping("/admin/review")
    public String getReview(Model model){
        List<ProductDetail> listpdt = productDetailRepository.findAll();
        List<Reviews> listr = reviewRepository.findAll();
        model.addAttribute("listpdt",listpdt);
        model.addAttribute("listr",listr);
        return "admin/reviews";
    }

//    @PostMapping("/admin/review/find")
//    public String find(@RequestParam("finding") Integer pdid, Model model){
//        if(pdid == 0) {
//            return "admin/reviews";
//        }
//        else{
//            List<ProductDetail> pdlist = productDetailRepository.findAll();
//            List<Reviews> listdg = reviewRepository.finddgbypdid(pdid);
//            model.addAttribute("listpdt", pdlist);
//            model.addAttribute("listr", listdg);
//            return "admin/reviews";
//        }
//    }

    @PostMapping("/admin/review/represent")
    public String represent(){
        return "redirect:/admin/review";
    }

    @GetMapping("/admin/reviews/delete")
    public String delete(@RequestParam("id") Integer id){
        reviewRepository.deleteById(id);
        return "redirect:/admin/review";
    }
}
