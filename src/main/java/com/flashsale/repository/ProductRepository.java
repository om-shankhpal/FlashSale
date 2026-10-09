package com.flashsale.repository;

import com.flashsale.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying
    @Query("""
    UPDATE Product p
    SET p.stock = p.stock - 1
    WHERE p.id = :productId AND p.stock > 0
    """)
    int decreaseStock(@Param("productId") Long productId);
}