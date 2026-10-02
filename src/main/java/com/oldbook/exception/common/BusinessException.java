package com.oldbook.exception.common;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}