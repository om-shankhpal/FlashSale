package com.flashsale.controller;

import com.flashsale.entity.Product;
import com.flashsale.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;
import com.flashsale.redis.RedisStockInitializationService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final RedisStockInitializationService redisStockInitializationService;

    public ProductController(ProductRepository productRepository, RedisStockInitializationService redisStockInitializationService) {
        this.productRepository = productRepository;
        this.redisStockInitializationService = redisStockInitializationService;
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        Product savedProduct = productRepository.save(product);

        redisStockInitializationService.initializeStock(savedProduct.getId());

        return savedProduct;
    }

    @GetMapping
    public List<Product> getProducts() {
        return productRepository.findAll();
    }
}