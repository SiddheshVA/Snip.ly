package com.urlshortener.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler — returns clean JSON error responses.
 * Without this, Spring returns HTML error pages which break the JS frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Handle @Valid validation errors (e.g. blank URL, bad email format) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errorBody("Validation failed", fieldErrors));
    }

    /** Handle business logic errors (alias taken, URL not found, etc.) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(errorBody(ex.getMessage(), null));
    }

    /** Handle wrong credentials */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody("Invalid email or password", null));
    }

    /** Handle permission errors */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(ex.getMessage(), null));
    }

    /** Catch-all for unexpected errors */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("An unexpected error occurred: " + ex.getMessage(), null));
    }

    private Map<String, Object> errorBody(String message, Object details) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("error", message);
        if (details != null) body.put("details", details);
        return body;
    }
}
