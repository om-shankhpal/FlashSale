
package com.flashsale.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisStockReservationServiceTest {

    private StringRedisTemplate redisTemplate;
    private RedisStockReservationService reservationService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        reservationService = new RedisStockReservationService(redisTemplate);
    }

    @Test
    void shouldReturnTrueWhenStockIsReserved() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(1L);

        assertTrue(reservationService.reserveStock(1L, "reservation-1"));
    }

    @Test
    void shouldReturnFalseWhenStockIsExhausted() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(0L);

        assertFalse(reservationService.reserveStock(1L, "reservation-2"));
    }

    @Test
    void shouldReturnTrueWhenReservationAlreadyExists() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(2L);

        assertTrue(reservationService.reserveStock(1L, "reservation-1"));
    }

    @Test
    void shouldThrowWhenStockIsNotInitialized() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(-1L);

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.reserveStock(1L, "reservation-3")
        );
    }

    @Test
    void shouldThrowWhenReservationIdBelongsToAnotherProduct() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(-2L);

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.reserveStock(1L, "reservation-1")
        );
    }

    @Test
    void shouldThrowWhenRedisStockValueIsInvalid() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(-3L);

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.reserveStock(1L, "reservation-4")
        );
    }

    @Test
    void shouldThrowWhenReservationIdExceedsMaximumLength() {
        String reservationId = "a".repeat(201);

        assertThrows(
                IllegalArgumentException.class,
                () -> reservationService.reserveStock(1L, reservationId)
        );

        verifyNoInteractions(redisTemplate);
    }
}