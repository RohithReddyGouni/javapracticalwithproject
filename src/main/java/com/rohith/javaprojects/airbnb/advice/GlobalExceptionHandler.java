package com.rohith.javaprojects.airbnb.advice;

import com.rohith.javaprojects.airbnb.exception.InvalidCredentialsException;
import com.rohith.javaprojects.airbnb.exception.InvalidRequestException;
import com.rohith.javaprojects.airbnb.exception.ResourceConflictException;
import com.rohith.javaprojects.airbnb.exception.ResourceNotFoundException;
import com.stripe.exception.StripeException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.task.TaskRejectedException;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException exception) {

        ApiError error = new ApiError(
                404,
                exception.getMessage(),
                Map.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiError> handleResourceConflict(ResourceConflictException exception) {

        ApiError apiError = new ApiError(
                409,
                exception.getMessage(),
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(apiError);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {

        ApiError apiError = new ApiError(
                401,
                exception.getMessage(),
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(apiError);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidRequestException exception) {

        ApiError apiError = new ApiError(
                400,
                exception.getMessage(),
                Map.of()
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ApiError> handleStripeError(
            StripeException exception) {

        ApiError error = new ApiError(
                502,
                "Payment provider request failed. Its outcome may require verification.",
                Map.of());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for(FieldError fieldError: exception.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ApiError apiError = new ApiError(
                400,
                "Validation failed",
                errors
        );
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDateIntegrityViolation(
            DataIntegrityViolationException exception){
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
            && "uk_inventory_room_date".equals(violation.getConstraintName())) {
                ApiError error = new ApiError(
                        409,
                        "Inventory already exists within the requested date range",
                        Map.of());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(error);
            }
            cause = cause.getCause();
        }

        throw exception;
    }

    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<ApiError> handleTaskRejected(
            TaskRejectedException exception) {

        ApiError error = new ApiError(
                503,
                "The server is busy. Please try again shortly.",
                Map.of());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }

    @ExceptionHandler({
            TimeoutException.class,
            AsyncRequestTimeoutException.class,
            QueryTimeoutException.class,
            TransactionTimedOutException.class
    })
    public ResponseEntity<ApiError> handleOperationTimeout(
            Exception exception) {

        ApiError error = new ApiError(
                503,
                "The operation timed out. Please try again.",
                Map.of());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }
}
