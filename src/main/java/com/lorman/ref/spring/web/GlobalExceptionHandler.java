package com.lorman.ref.spring.web;

import com.lorman.ref.spring.dto.ErrorResponseDTO;
import com.lorman.ref.spring.exception.NotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static String rootCause(Throwable t) {
        Throwable curr = t;
        while (curr.getCause() != null && curr.getCause() != curr) {
            curr = curr.getCause();
        }
        return Optional.ofNullable(curr.getMessage()).orElse(curr.getClass().getSimpleName());
    }

    private static ErrorResponseDTO body(HttpStatus status, String message, String cause, ServerWebExchange exchange) {
        return new ErrorResponseDTO(
                status.value(),
                message,
                cause,
                exchange.getRequest().getPath().value(),
                Instant.now()
        );
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(NotFoundException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(HttpStatus.NOT_FOUND, ex.getMessage(), rootCause(ex), exchange));
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception ex, ServerWebExchange exchange) {
        String msg = Optional.ofNullable(ex.getMessage()).filter(s -> !s.isBlank()).orElse("Bad request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, msg, rootCause(ex), exchange));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationBind(WebExchangeBindException ex, ServerWebExchange exchange) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + Optional.ofNullable(fe.getDefaultMessage()).orElse("invalid"))
                .collect(Collectors.joining(", "));
        String msg = details.isBlank() ? "Validation failed" : "Validation failed: " + details;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, msg, rootCause(ex), exchange));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        String details = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        String msg = details.isBlank() ? "Constraint violation" : "Constraint violation: " + details;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, msg, rootCause(ex), exchange));
    }

    // Security removed: no dedicated handlers for AccessDenied/Authentication exceptions

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("Unhandled exception on {}: {}", exchange.getRequest().getPath(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", ex.getClass().getSimpleName(), exchange));
    }
}
