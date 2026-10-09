
package com.flashsale.service;

import com.flashsale.entity.Order;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public PurchaseService(
            ProductRepository productRepository,
            OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public String purchase(Long productId) {
        int updatedRows = productRepository.decreaseStock(productId);

        if (updatedRows == 0) {
            throw new RuntimeException("Product not found or out of stock");
        }

        Order order = new Order();
        order.setProductId(productId);

        orderRepository.save(order);

        return "Purchase successful";
    }
}