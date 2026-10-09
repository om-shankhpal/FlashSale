package com.flashsale.service;

import com.flashsale.entity.Product;
import com.flashsale.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final ProductRepository productRepository;

    public PurchaseService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public String purchase(Long productId) {
        int updatedRows = productRepository.decreaseStock(productId);

        if (updatedRows == 0) {
            throw new RuntimeException("Product not found or out of stock");
        }

        return "Purchase successful";
    }
}