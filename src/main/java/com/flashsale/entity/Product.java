
package com.flashsale.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant saleStartsAt;

    @Column(nullable = false)
    private Instant saleEndsAt;
}