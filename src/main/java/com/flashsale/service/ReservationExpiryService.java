package com.flashsale.service;

import com.flashsale.entity.Order;
import com.flashsale.entity.OrderStatus;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationExpiryService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public ReservationExpiryService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public int expireReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<Order> expiredOrders = orderRepository.findExpiredOrders(
                OrderStatus.PENDING_PAYMENT, now);

        int expiredCount = 0;

        for (Order order : expiredOrders) {
            int updatedRows = entityManager.createNativeQuery("""
                    UPDATE orders
                    SET status = 'EXPIRED',
                        version = version + 1
                    WHERE id = :orderId
                      AND status = 'PENDING_PAYMENT'
                      AND reservation_expires_at <= :now
                    """)
                    .setParameter("orderId", order.getId())
                    .setParameter("now", now)
                    .executeUpdate();

            if (updatedRows == 1) {
                productRepository.increaseStock(order.getProductId());
                expiredCount++;
            }
        }

        return expiredCount;
    }
    @Scheduled(fixedDelayString = "${flashsale.reservation.expiry-interval-ms:10000}")
    public void scheduledExpiry() {
        expireReservations();
    }
}