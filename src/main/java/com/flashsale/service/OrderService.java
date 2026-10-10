package com.flashsale.service;

import com.flashsale.entity.Order;
import com.flashsale.entity.OrderStatus;
import com.flashsale.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;

    public OrderService(
            OrderRepository orderRepository,
            OrderStateMachine stateMachine) {
        this.orderRepository = orderRepository;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public Order transitionStatus(Long orderId, OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Order not found"));

        stateMachine.validateTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        return orderRepository.save(order);
    }
}