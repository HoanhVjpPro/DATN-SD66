package com.example.datnhathub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Review", uniqueConstraints = @UniqueConstraint(
        name = "UQ_Review_ProductDetail_Customer",
        columnNames = {"ProductDetailID", "CustomerID"}
))
public class Reviews {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ReviewID")
    public Integer ReviewID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductDetailID",referencedColumnName = "ProductDetailID")
    public ProductDetail ProductDetailID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID",referencedColumnName = "CustomerID")
    public Customer CustomerID;

    @Column(name = "Rating")
    public Double Rating;

    @Column(name = "Comment")
    public String Comment;

    @Column(name = "AdminReply", columnDefinition = "NVARCHAR(MAX)")
    public String adminReply;
}
