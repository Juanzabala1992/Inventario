package com.technicaltest.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(ProductNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleProductNotFound(ProductNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "Product not found", exception.getMessage());
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    ResponseEntity<Map<String, Object>> handleInsufficientInventory(
            InsufficientInventoryException exception
    ) {
        return error(HttpStatus.CONFLICT, "Insufficient inventory", exception.getMessage());
    }

    @ExceptionHandler(ProductServiceUnavailableException.class)
    ResponseEntity<Map<String, Object>> handleProductServiceUnavailable(
            ProductServiceUnavailableException exception
    ) {
        return error(HttpStatus.BAD_GATEWAY, "Product service unavailable", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception) {
        String detail = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "))
                : exception.getMessage();

        return error(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "Unexpected service error.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String title, String detail) {
        return ResponseEntity
                .status(status)
                .contentType(JsonApi.JSON_API_MEDIA_TYPE)
                .body(JsonApi.errorDocument(status.value(), title, detail));
    }
}

