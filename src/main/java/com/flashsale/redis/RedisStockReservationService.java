package com.flashsale.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedisStockReservationService {

    private static final String STOCK_KEY_PREFIX = "flashsale:stock:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveStockScript;

    public RedisStockReservationService(
            StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.reserveStockScript = new DefaultRedisScript<>();
        this.reserveStockScript.setLocation(
                new ClassPathResource("scripts/reserve-stock.lua")
        );
        this.reserveStockScript.setResultType(Long.class);
    }

    public boolean reserveStock(Long productId) {
        String stockKey = STOCK_KEY_PREFIX + productId;

        Long result = redisTemplate.execute(
                reserveStockScript,
                List.of(stockKey)
        );

        if (result == null) {
            throw new IllegalStateException(
                    "Redis returned no result for stock reservation"
            );
        }

        return switch (result.intValue()) {
            case 1 -> true;
            case 0 -> false;
            case -1 -> throw new IllegalStateException(
                    "Redis stock has not been initialized for product "
                            + productId
            );
            default -> throw new IllegalStateException(
                    "Unexpected stock reservation result: " + result
            );
        };
    }
}