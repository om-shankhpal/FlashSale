package com.flashsale.service;

import com.flashsale.entity.Order;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.flashsale.exception.OutOfStockException;

@Service
public class PurchaseService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EntityManager entityManager;

    public PurchaseService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            EntityManager entityManager) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public String purchase(Long productId, String idempotencyKey) {

        // Serialize requests that use the same idempotency key.
        entityManager.createNativeQuery("""
                SELECT pg_advisory_xact_lock(
                    hashtextextended(:idempotencyKey, 0)
                )
                """)
                .setParameter("idempotencyKey", idempotencyKey)
                .getSingleResult();

        // Check again after acquiring the lock.
        if (orderRepository.existsByIdempotencyKey(idempotencyKey)) {
            return "Purchase already processed";
        }

        int updatedRows = productRepository.decreaseStock(productId);

        if (updatedRows == 0) {
            throw new OutOfStockException(
                    "Product not found or out of stock");
        }

        Order order = new Order();
        order.setProductId(productId);
        order.setIdempotencyKey(idempotencyKey);

        orderRepository.save(order);

        return "Purchase successful";
    }
}