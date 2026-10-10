package com.flashsale;

import com.flashsale.entity.Order;
import com.flashsale.entity.OrderStatus;
import com.flashsale.entity.Product;
import com.flashsale.exception.OutOfStockException;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import com.flashsale.service.PurchaseService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setStock(10);
        product.setSaleStartsAt(LocalDateTime.now().minusMinutes(5));
        product.setSaleEndsAt(LocalDateTime.now().plusHours(1));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        purchaseService = new PurchaseService(
                productRepository,
                orderRepository,
                entityManager
        );
    }

    @Test
    void shouldCreateOrderWhenStockIsAvailable() {
        when(orderRepository.findByIdempotencyKey("test-key"))
                .thenReturn(Optional.empty());
        when(productRepository.decreaseStock(1L))
                .thenReturn(1);
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = purchaseService.purchase(1L, "test-key");

        assertEquals(OrderStatus.PENDING_PAYMENT, result.getStatus());
        assertEquals(1L, result.getProductId());
        assertEquals("test-key", result.getIdempotencyKey());

        verify(productRepository).decreaseStock(1L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldReturnExistingOrderWithoutDeductingStock() {
        Order existingOrder = new Order();
        existingOrder.setProductId(1L);
        existingOrder.setIdempotencyKey("test-key");
        existingOrder.setStatus(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findByIdempotencyKey("test-key"))
                .thenReturn(Optional.of(existingOrder));

        Order result = purchaseService.purchase(1L, "test-key");

        assertEquals(existingOrder, result);

        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).decreaseStock(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void shouldRejectPurchaseWhenStockIsUnavailable() {
        when(orderRepository.findByIdempotencyKey("out-of-stock-key"))
                .thenReturn(Optional.empty());
        when(productRepository.decreaseStock(1L))
                .thenReturn(0);

        assertThrows(
                OutOfStockException.class,
                () -> purchaseService.purchase(1L, "out-of-stock-key")
        );

        verify(productRepository).findById(1L);
        verify(productRepository).decreaseStock(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }
}