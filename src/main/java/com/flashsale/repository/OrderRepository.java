
package com.flashsale.repository;

import com.flashsale.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import com.flashsale.entity.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("""
    SELECT o FROM Order o
    WHERE o.status = :status
      AND o.reservationExpiresAt <= :now
    """)
    List<Order> findExpiredOrders(
            @Param("status") OrderStatus status,
            @Param("now") LocalDateTime now
    );
}