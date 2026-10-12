package com.flashsale.service;

import com.flashsale.entity.Order;
import com.flashsale.entity.OrderStatus;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ReservationExpiryServiceTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private EntityManager entityManager;
    private Query updateQuery;
    private ReservationExpiryService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        entityManager = mock(EntityManager.class);
        updateQuery = mock(Query.class);

        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(updateQuery);
        when(updateQuery.setParameter(anyString(), any()))
                .thenReturn(updateQuery);

        service = new ReservationExpiryService(
                orderRepository,
                productRepository,
                entityManager
        );
    }

    @Test
    void shouldExpireOrderAndRestoreStock() {
        Order order = new Order();
        order.setId(1L);
        order.setProductId(10L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setReservationExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(orderRepository.findExpiredOrders(
                eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(order));

        when(updateQuery.executeUpdate()).thenReturn(1);
        when(productRepository.increaseStock(10L)).thenReturn(1);

        int result = service.expireReservations();

        assertEquals(1, result);
        verify(productRepository).increaseStock(10L);
    }

    @Test
    void shouldNotRestoreStockWhenAnotherWorkerAlreadyExpiredOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setProductId(10L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setReservationExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(orderRepository.findExpiredOrders(
                eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of(order));

        when(updateQuery.executeUpdate()).thenReturn(0);

        int result = service.expireReservations();

        assertEquals(0, result);
        verify(productRepository, never()).increaseStock(any());
    }

    @Test
    void shouldDoNothingWhenNoOrdersHaveExpired() {
        when(orderRepository.findExpiredOrders(
                eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(List.of());

        int result = service.expireReservations();

        assertEquals(0, result);
        verifyNoInteractions(entityManager);
        verifyNoInteractions(productRepository);
    }
}