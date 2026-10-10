package com.flashsale.controller;

import com.flashsale.exception.OutOfStockException;
import com.flashsale.exception.SaleNotActiveException;
import com.flashsale.service.PurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(PurchaseController.class)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PurchaseService purchaseService;

    @Test
    void shouldReturnConflictWhenSaleIsNotActive() throws Exception {
        when(purchaseService.purchase(1L, "test-key"))
                .thenThrow(new SaleNotActiveException(
                        "The sale is not currently active"));

        mockMvc.perform(post("/api/purchases/1")
                        .header("Idempotency-Key", "test-key"))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "The sale is not currently active"));
    }

    @Test
    void shouldReturnConflictWhenStockIsUnavailable() throws Exception {
        when(purchaseService.purchase(1L, "test-key"))
                .thenThrow(new OutOfStockException(
                        "Product is out of stock"));

        mockMvc.perform(post("/api/purchases/1")
                        .header("Idempotency-Key", "test-key"))
                .andExpect(status().isConflict())
                .andExpect(content().string("Product is out of stock"));
    }
}