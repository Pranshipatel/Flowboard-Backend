package com.flowboard.board.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {

    // HTTP status for the error
    private final HttpStatus status;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}