package com.flashsale.controller;

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
    public ResponseEntity<String> purchase(
            @PathVariable Long productId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        try {
            return ResponseEntity.ok(
                    purchaseService.purchase(productId, idempotencyKey)
            );
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }
}