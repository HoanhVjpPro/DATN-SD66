package com.example.datnhathub.repository;

import com.example.datnhathub.dto.ReviewDTO;
import com.example.datnhathub.entity.Reviews;
import com.example.datnhathub.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Reviews,Integer> {

    @Query(value = """
    select u.Username as CustomerName, r.Rating as Rating, r.Comment as Comment, r.AdminReply as AdminReply
    from Review r
    join Customer c on r.CustomerID = c.CustomerID
    join Users u on c.UserID = u.UserID
    join Product_Detail pd on r.ProductDetailID = pd.ProductDetailID
    join Product p on p.ProductID = pd.ProductID
    where p.ProductID = ?1
""", nativeQuery = true)
    List<ReviewDTO> findcomment(Integer Productdetailid);

    @Transactional
    @Modifying
    @Query(value = """
        select * from Users where Username like = ?1
    """,nativeQuery = true)
    Users finduserbyname(String username);

    @Query(value = """
        select TOP 1 * from Product_Detail where ProductID = ?1 order by ProductDetailID ASC
    """,nativeQuery = true)
    Integer findbyproductID(Integer ProductID);

    @Query(value = """
        select count(ReviewID) from Review where CustomerID = ?1 and ProductDetailID = ?2
    """,nativeQuery = true)
    Integer countbycustomerandproduct(Integer CustomerID, Integer ProductdetailID);

    @Transactional
    @Modifying
    @Query(value = """
        update Review 
        set Rating = ?1, Comment = ?2 where ReviewID = ?3
    """,nativeQuery = true)
    void updatecomment(Integer Rating, String Comment, Integer ReviewID);


    @Query(value = """
        select * from Review where CustomerID = ?1 and ProductDetailID = ?2
    """,nativeQuery = true)
    Reviews findByCustomerIDAndProductDetailID(Integer CustomerID, Integer ProductdetailID);

    @Transactional
    @Modifying
    @Query(value = """
        select ProductDetailID from Product_Detail where ProductID = ?1
    """,nativeQuery = true)
    ArrayList<Integer> finddspdid(Integer ProductDetailID);

    @Transactional
    @Modifying
    @Query(value = """
        select * from Review where ProductDetailID = ?1
    """,nativeQuery = true)
    ArrayList<Reviews> finddgbypdid(Integer ProductDetailID);
}
