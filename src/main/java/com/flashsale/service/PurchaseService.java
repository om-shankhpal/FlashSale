package com.flashsale.service;

import com.flashsale.entity.Order;
import com.flashsale.entity.OrderStatus;
import com.flashsale.entity.Product;
import com.flashsale.exception.OutOfStockException;
import com.flashsale.exception.SaleNotActiveException;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.flashsale.redis.RedisStockInitializationService;

import java.time.LocalDateTime;

@Service
public class PurchaseService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final EntityManager entityManager;
    private final RedisStockInitializationService redisStockInitializationService;

    public PurchaseService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            EntityManager entityManager,
            RedisStockInitializationService redisStockInitializationService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.entityManager = entityManager;
        this.redisStockInitializationService = redisStockInitializationService;
    }

    @Transactional
    public Order purchase(Long productId, String idempotencyKey) {

        entityManager.createNativeQuery("""
                SELECT pg_advisory_xact_lock(
                    hashtextextended(:idempotencyKey, 0)
                )
                """)
                .setParameter("idempotencyKey", idempotencyKey)
                .getSingleResult();

        var existingOrder =
                orderRepository.findByIdempotencyKey(idempotencyKey);

        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new OutOfStockException("Product not found"));

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(product.getSaleStartsAt())
                || !now.isBefore(product.getSaleEndsAt())) {
            throw new SaleNotActiveException(
                    "The sale is not currently active");
        }

        if (!redisStockInitializationService.isStockAvailable(productId)) {
            throw new OutOfStockException("Product is out of stock");
        }

        int updatedRows = productRepository.decreaseStock(productId);

        if (updatedRows == 0) {
            throw new OutOfStockException(
                    "Product is out of stock");
        }

        Order order = new Order();
        order.setProductId(productId);
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setReservationExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );

        return orderRepository.save(order);
    }
}