package com.flashsale;

import com.flashsale.entity.Order;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import com.flashsale.service.PurchaseService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PurchaseServiceTest {

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private EntityManager entityManager;
    private Query lockQuery;
    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        orderRepository = mock(OrderRepository.class);
        entityManager = mock(EntityManager.class);
        lockQuery = mock(Query.class);

        when(entityManager.createNativeQuery(anyString()))
                .thenReturn(lockQuery);
        when(lockQuery.setParameter(anyString(), any()))
                .thenReturn(lockQuery);
        when(lockQuery.getSingleResult())
                .thenReturn(null);

        purchaseService = new PurchaseService(
                productRepository,
                orderRepository,
                entityManager
        );
    }

    @Test
    void shouldCreateOrderWhenStockIsAvailable() {
        when(orderRepository.existsByIdempotencyKey("test-key"))
                .thenReturn(false);
        when(productRepository.decreaseStock(1L))
                .thenReturn(1);

        String result = purchaseService.purchase(1L, "test-key");

        assertEquals("Purchase successful", result);
        verify(productRepository).decreaseStock(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldNotDeductStockWhenKeyWasAlreadyProcessed() {
        when(orderRepository.existsByIdempotencyKey("test-key"))
                .thenReturn(true);

        String result = purchaseService.purchase(1L, "test-key");

        assertEquals("Purchase already processed", result);
        verify(productRepository, never()).decreaseStock(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldRejectPurchaseWhenStockIsUnavailable() {
        when(orderRepository.existsByIdempotencyKey("out-of-stock-key"))
                .thenReturn(false);
        when(productRepository.decreaseStock(1L))
                .thenReturn(0);

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> purchaseService.purchase(1L, "out-of-stock-key")
        );

        verify(productRepository).decreaseStock(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }
}