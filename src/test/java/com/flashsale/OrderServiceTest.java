package com.flashsale;

import com.flashsale.entity.Order;
import com.flashsale.entity.OrderStatus;
import com.flashsale.repository.OrderRepository;
import com.flashsale.service.OrderService;
import com.flashsale.service.OrderStateMachine;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private OrderStateMachine stateMachine;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        stateMachine = new OrderStateMachine();
        orderService = new OrderService(orderRepository, stateMachine);
    }

    @Test
    void shouldTransitionFromPendingPaymentToPaid() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.transitionStatus(1L, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void shouldRejectInvalidTransition() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.transitionStatus(1L, OrderStatus.CONFIRMED)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> orderService.transitionStatus(99L, OrderStatus.PAID)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }
}