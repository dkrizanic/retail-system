package com.retailsystem.cart.exception;

public class InvalidCartStateException extends RuntimeException {

    public InvalidCartStateException(String message) {
        super(message);
    }
}
