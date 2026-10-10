package com.flashsale.service;

import com.flashsale.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    OrderStatus.PENDING_PAYMENT,
                    Set.of(
                            OrderStatus.PAID,
                            OrderStatus.EXPIRED,
                            OrderStatus.CANCELLED,
                            OrderStatus.FAILED
                    ),

                    OrderStatus.PAID,
                    Set.of(OrderStatus.CONFIRMED)
            );

    public boolean canTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus) {

        return ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(newStatus);
    }

    public void validateTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus) {

        if (!canTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                    "Invalid order status transition: "
                            + currentStatus + " -> " + newStatus
            );
        }
    }
}