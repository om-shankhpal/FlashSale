package com.flashsale.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

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
                anyList()
        )).thenReturn(1L);

        assertTrue(reservationService.reserveStock(1L));
    }

    @Test
    void shouldReturnFalseWhenStockIsExhausted() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList()
        )).thenReturn(0L);

        assertFalse(reservationService.reserveStock(1L));
    }

    @Test
    void shouldThrowWhenStockIsNotInitialized() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList()
        )).thenReturn(-1L);

        assertThrows(
                IllegalStateException.class,
                () -> reservationService.reserveStock(1L)
        );
    }
}