package com.flowboard.auth.exception;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 *
 * Handles all exceptions across the application in a centralized way.
 * Ensures consistent error response structure.
 *
 * Responsibilities:
 * - Handle custom business exceptions
 * - Handle validation errors (@Valid)
 * - Handle unexpected system exceptions
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles CustomException (application-specific exceptions)
     *
     * Builds a structured error response using:
     * - Timestamp
     * - HTTP status code
     * - Error reason
     * - Custom message
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage()
        );

        return new ResponseEntity<>(error, ex.getStatus());
    }

    /**
     * Handles validation failures for @Valid annotated DTOs
     *
     * Collects all field-level validation errors
     * and combines them into a single readable message
     *
     * Example output:
     * "email: must be valid, password: must not be blank"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                message
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles all uncaught exceptions (fallback handler)
     *
     * Used for unexpected errors in the system.
     * Prevents stack traces from leaking to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}