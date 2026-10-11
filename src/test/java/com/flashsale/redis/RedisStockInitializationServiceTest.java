package com.flashsale.redis;

import com.flashsale.entity.Product;
import com.flashsale.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RedisStockInitializationServiceTest {

    private StringRedisTemplate redisTemplate;
    private ProductRepository productRepository;
    private ValueOperations<String, String> valueOperations;
    private RedisStockInitializationService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        productRepository = mock(ProductRepository.class);
        valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new RedisStockInitializationService(
                redisTemplate, productRepository
        );
    }

    @Test
    void shouldInitializeStockWhenRedisKeyDoesNotExist() {
        Product product = mock(Product.class);
        when(product.getStock()).thenReturn(10);
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        service.initializeStock(1L);

        verify(valueOperations).setIfAbsent("flashsale:stock:1", "10");
    }

    @Test
    void shouldNotOverwriteExistingRedisStock() {
        Product product = mock(Product.class);
        when(product.getStock()).thenReturn(10);
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(valueOperations.setIfAbsent("flashsale:stock:1", "10"))
                .thenReturn(false);

        assertDoesNotThrow(() -> service.initializeStock(1L));

        verify(valueOperations).setIfAbsent("flashsale:stock:1", "10");
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.initializeStock(99L)
        );

        verifyNoInteractions(redisTemplate);
    }
}