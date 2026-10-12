package com.flashsale;

import com.flashsale.entity.Order;
import com.flashsale.entity.Product;
import com.flashsale.repository.OrderRepository;
import com.flashsale.repository.ProductRepository;
import com.flashsale.service.PurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class PurchaseLoadTest {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void shouldNeverOversellUnderConcurrentPurchases() throws Exception {
        int initialStock = 20;
        int requestCount = 100;

        Product product = new Product();
        product.setName("Load Test Product");
        product.setStock(initialStock);
        product.setPrice(new java.math.BigDecimal("10.00"));
        product.setSaleStartsAt(LocalDateTime.now().minusMinutes(1));
        product.setSaleEndsAt(LocalDateTime.now().plusHours(1));

        product = productRepository.saveAndFlush(product);
        Long productId = product.getId();

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successful = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        long startedAt = System.nanoTime();

        try {
            List<Future<?>> futures = new java.util.ArrayList<>();

            for (int i = 0; i < requestCount; i++) {
                final int requestId = i;

                futures.add(executor.submit(() -> {
                    ready.countDown();

                    try {
                        start.await();

                        Order order = purchaseService.purchase(
                                productId,
                                "load-test-" + requestId
                        );

                        successful.incrementAndGet();
                    } catch (Exception exception) {
                        rejected.incrementAndGet();
                    }
                }));
            }

//            ready.await();
            start.countDown();

            for (Future<?> future : futures) {
                future.get(2, TimeUnit.MINUTES);
            }
        } finally {
            executor.shutdownNow();
        }

        long durationMillis =
                TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startedAt
                );

        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow();

        System.out.println("Total requests: " + requestCount);
        System.out.println("Successful purchases: " + successful.get());
        System.out.println("Rejected purchases: " + rejected.get());
        System.out.println("Remaining stock: " + updatedProduct.getStock());
        System.out.println("Duration (ms): " + durationMillis);
        System.out.println("Throughput (requests/sec): "
                + (requestCount * 1000.0 / Math.max(durationMillis, 1)));

        assertEquals(initialStock, successful.get());
        assertEquals(0, updatedProduct.getStock());
        assertEquals(requestCount, successful.get() + rejected.get());
    }
}