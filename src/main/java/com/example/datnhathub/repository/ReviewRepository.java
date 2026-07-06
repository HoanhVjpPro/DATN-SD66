package com.example.datnhathub.repository;

import com.example.datnhathub.dto.ReviewDTO;
import com.example.datnhathub.entity.Reviews;
import com.example.datnhathub.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Reviews,Integer> {

    @Transactional
    @Modifying
    @Query(value = """
        select u.Username,r.Rating,r.Comment from Review r join Customer c on r.CustomerID = c.CustomerID join Users u on c.UserID = u.UserID join Product_Detail pd on r.ProductDetailID = pd.ProductDetailID join Product p on p.ProductID = pd.ProductID where p.ProductID = ?1
    """,nativeQuery = true)
    List<ReviewDTO> findcomment(Integer Productdetailid);

    @Transactional
    @Modifying
    @Query(value = """
        select * from Users where Username like = ?1
    """,nativeQuery = true)
    Users finduserbyname(String username);
}
