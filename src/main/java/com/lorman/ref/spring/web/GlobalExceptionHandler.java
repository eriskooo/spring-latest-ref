package com.lorman.ref.spring.web;

import com.lorman.ref.spring.dto.ErrorResponseDTO;
import com.lorman.ref.spring.exception.NotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;

import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static String rootCause(Throwable t) {
        Throwable curr = t;
        while (curr.getCause() != null && curr.getCause() != curr) {
            curr = curr.getCause();
        }
        return Optional.ofNullable(curr.getMessage()).orElse(curr.getClass().getSimpleName());
    }

    private static String messageOrDefault(Throwable t, String def) {
        return Optional.ofNullable(t.getMessage()).filter(s -> !s.isBlank()).orElse(def);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(ex.getMessage(), rootCause(ex)));
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(messageOrDefault(ex, "Bad request"), rootCause(ex)));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationBind(WebExchangeBindException ex) {
        String details = ex.getAllErrors().stream()
                .map(err -> {
                    String field = err.getObjectName();
                    try {
                        // FieldError available for field-specific errors
                        field = ex.getBindingResult().getFieldError() != null ? ex.getBindingResult().getFieldError().getField() : field;
                    } catch (Exception ignore) {
                    }
                    return field + ": " + Optional.ofNullable(err.getDefaultMessage()).orElse("invalid");
                })
                .collect(Collectors.joining(", "));
        String msg = details.isBlank() ? "Validation failed" : "Validation failed: " + details;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(msg, rootCause(ex)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex) {
        String details = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        String msg = details.isBlank() ? "Constraint violation" : "Constraint violation: " + details;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(msg, rootCause(ex)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(messageOrDefault(ex, "Internal server error"), rootCause(ex)));
    }
}
