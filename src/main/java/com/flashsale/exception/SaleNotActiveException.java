package com.flashsale.exception;

public class SaleNotActiveException extends RuntimeException {

    public SaleNotActiveException(String message) {
        super(message);
    }
}