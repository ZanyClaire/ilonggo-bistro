package com.ilonggobistro.delivery;

public class OutOfCoverageException extends RuntimeException {
    public OutOfCoverageException(String message) {
        super(message);
    }
}
