
package com.flashsale.controller;

import com.flashsale.entity.Order;
import com.flashsale.exception.OutOfStockException;
import com.flashsale.exception.SaleNotActiveException;
import com.flashsale.service.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping("/{productId}")
    public ResponseEntity<?> purchase(
            @PathVariable Long productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        try {
            Order order = purchaseService.purchase(
                    productId, idempotencyKey);

            return ResponseEntity.ok(order);

        } catch (OutOfStockException | SaleNotActiveException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }
}