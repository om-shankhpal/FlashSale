
package com.flashsale.integration;

import com.flashsale.entity.Order;
import com.flashsale.entity.Product;
import com.flashsale.exception.OutOfStockException;
import com.flashsale.redis.RedisStockInitializationService;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import com.flashsale.service.PurchaseService;
import com.flashsale.service.ReservationExpiryService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class PurchaseConcurrencyIntegrationTest {

    @MockitoBean
    private RedisStockInitializationService redisStockInitializationService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ReservationExpiryService reservationExpiryService;

    private Long productId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();

        Mockito.when(
                redisStockInitializationService.isStockAvailable(Mockito.anyLong())
        ).thenReturn(true);

        Product product = new Product();
        product.setName("Concurrency Test Product");
        product.setStock(5);
        product.setPrice(new BigDecimal("100.00"));
        product.setSaleStartsAt(
                Instant.now().minus(5, ChronoUnit.MINUTES)
        );
        product.setSaleEndsAt(
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        productId = productRepository.saveAndFlush(product).getId();
    }

    @Test
    void shouldNeverOversellWhenPurchasesAreConcurrent() throws Exception {
        int concurrentRequests = 20;
        int initialStock = 5;

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrentRequests);

        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();

            for (int i = 0; i < concurrentRequests; i++) {
                final int requestNumber = i;

                tasks.add(() -> {
                    try {
                        Order order = purchaseService.purchase(
                                productId,
                                "concurrent-key-" + requestNumber
                        );
                        return order != null;
                    } catch (OutOfStockException ex) {
                        return false;
                    }
                });
            }

            List<Future<Boolean>> results = executor.invokeAll(tasks);

            long successfulPurchases = 0;

            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successfulPurchases++;
                }
            }

            Product finalProduct = productRepository.findById(productId)
                    .orElseThrow();

            assertEquals(
                    (long) initialStock - successfulPurchases,
                    finalProduct.getStock().longValue()
            );
            assertTrue(finalProduct.getStock() >= 0);
            assertTrue(successfulPurchases <= initialStock);

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldCreateOnlyOneOrderForConcurrentRequestsWithSameIdempotencyKey()
            throws Exception {

        int concurrentRequests = 20;
        String sharedIdempotencyKey = "same-key-concurrent-test";

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrentRequests);

        try {
            List<Callable<Long>> tasks = new ArrayList<>();

            for (int i = 0; i < concurrentRequests; i++) {
                tasks.add(() -> {
                    Order order = purchaseService.purchase(
                            productId,
                            sharedIdempotencyKey
                    );
                    return order.getId();
                });
            }

            List<Future<Long>> results = executor.invokeAll(tasks);

            Long expectedOrderId = results.get(0).get();

            for (Future<Long> result : results) {
                assertEquals(expectedOrderId, result.get());
            }

            Product finalProduct = productRepository.findById(productId)
                    .orElseThrow();

            assertEquals(4, finalProduct.getStock());
            assertEquals(1, orderRepository.count());

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldExpireOrderAndRestoreStockExactlyOnce() {
        Product product = productRepository.findById(productId)
                .orElseThrow();

        Order order = new Order();
        order.setProductId(productId);
        order.setIdempotencyKey("expiry-integration-key");
        order.setStatus(
                com.flashsale.entity.OrderStatus.PENDING_PAYMENT
        );
        order.setReservationExpiresAt(
                Instant.now().minus(1, ChronoUnit.MINUTES)
        );

        orderRepository.saveAndFlush(order);

        int firstRun = reservationExpiryService.expireReservations();

        Product afterFirstRun = productRepository.findById(productId)
                .orElseThrow();

        Order expiredOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertEquals(1, firstRun);
        assertEquals(6, afterFirstRun.getStock());
        assertEquals(
                com.flashsale.entity.OrderStatus.EXPIRED,
                expiredOrder.getStatus()
        );

        int secondRun = reservationExpiryService.expireReservations();

        Product afterSecondRun = productRepository.findById(productId)
                .orElseThrow();

        assertEquals(0, secondRun);
        assertEquals(6, afterSecondRun.getStock());
    }
}