package com.jobflow.orchestrator.exception;

import com.jobflow.orchestrator.workflow.WorkflowDagValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(Instant timestamp, int status, String error, String message) {
        static ErrorBody of(HttpStatus status, String message) {
            return new ErrorBody(Instant.now(), status.value(), status.getReasonPhrase(), message);
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorBody> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorBody.of(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, WorkflowDagValidator.WorkflowValidationException.class})
    public ResponseEntity<ErrorBody> handleBadRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorBody.of(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorBody.of(HttpStatus.BAD_REQUEST, message));
    }
}
