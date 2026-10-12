
package com.flashsale.redis;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedisStockReservationService {

    private static final String STOCK_KEY_PREFIX = "flashsale:stock:";
    private static final String RESERVATION_KEY_PREFIX =
            "flashsale:reservation:";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> reserveStockScript;

    public RedisStockReservationService(
            StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.reserveStockScript = new DefaultRedisScript<>();
        this.reserveStockScript.setLocation(
                new ClassPathResource("scripts/reserve-stock-with-id.lua")
        );
        this.reserveStockScript.setResultType(Long.class);
    }

    public boolean reserveStock(Long productId, String reservationId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException(
                    "Product ID must be positive"
            );
        }

        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Reservation ID must not be blank"
            );
        }

        String stockKey = STOCK_KEY_PREFIX + productId;
        String reservationKey =
                RESERVATION_KEY_PREFIX + reservationId;

        Long result = redisTemplate.execute(
                reserveStockScript,
                List.of(stockKey, reservationKey),
                productId.toString(),
                reservationId
        );

        if (result == null) {
            throw new IllegalStateException(
                    "Redis returned no result for stock reservation"
            );
        }

        return switch (result.intValue()) {
            case 1, 2 -> true;
            case 0 -> false;
            case -1 -> throw new IllegalStateException(
                    "Redis stock has not been initialized for product "
                            + productId
            );
            case -2 -> throw new IllegalStateException(
                    "Reservation ID is already associated with another product"
            );
            default -> throw new IllegalStateException(
                    "Unexpected Redis reservation result: " + result
            );
        };
    }
}