
package com.flashsale.redis;

import com.flashsale.entity.Product;
import com.flashsale.repository.ProductRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisStockInitializationService {

    private static final String STOCK_KEY_PREFIX = "flashsale:stock:";

    private final StringRedisTemplate redisTemplate;
    private final ProductRepository productRepository;

    public RedisStockInitializationService(
            StringRedisTemplate redisTemplate,
            ProductRepository productRepository) {
        this.redisTemplate = redisTemplate;
        this.productRepository = productRepository;
    }

    public void initializeStock(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found: " + productId
                ));

        String stockKey = STOCK_KEY_PREFIX + productId;

        redisTemplate.opsForValue().setIfAbsent(
                stockKey,
                String.valueOf(product.getStock())
        );
    }

    public boolean isStockAvailable(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        String stock = redisTemplate.opsForValue().get(stockKey);

        // Missing cache data must not block a purchase.
        if (stock == null) {
            return true;
        }

        try {
            return Long.parseLong(stock) > 0;
        } catch (NumberFormatException exception) {
            return true;
        }
    }
}