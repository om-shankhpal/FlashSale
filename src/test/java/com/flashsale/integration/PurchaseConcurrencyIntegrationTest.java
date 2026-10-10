package com.flashsale.integration;

import com.flashsale.entity.Product;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import com.flashsale.service.PurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import com.flashsale.entity.Order;
import com.flashsale.exception.OutOfStockException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class PurchaseConcurrencyIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PurchaseService purchaseService;

    private Long productId;

    @BeforeEach
//    @Transactional
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();

        Product product = new Product();
        product.setName("Concurrency Test Product");
        product.setStock(5);
        product.setPrice(new BigDecimal("100.00"));
        product.setSaleStartsAt(LocalDateTime.now().minusMinutes(5));
        product.setSaleEndsAt(LocalDateTime.now().plusHours(1));

        productId = productRepository.saveAndFlush(product).getId();
    }

    @Test
    void shouldNeverOversellWhenPurchasesAreConcurrent() throws Exception {
        int concurrentRequests = 20;
        int initialStock = 5;

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);

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
}