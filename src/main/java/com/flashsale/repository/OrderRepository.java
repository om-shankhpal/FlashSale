
package com.flashsale.repository;

import com.flashsale.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}