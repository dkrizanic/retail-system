package com.retailsystem.cart.exception;

import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        return problemDetail;
    }

    @ExceptionHandler(InvalidCartStateException.class)
    public ProblemDetail handleInvalidCartState(InvalidCartStateException ex) {
        log.warn("Invalid cart state: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Cart State Conflict");
        return problemDetail;
    }

    @ExceptionHandler({DuplicateKeyException.class, OptimisticLockingFailureException.class})
    public ProblemDetail handleConcurrencyConflict(RuntimeException ex) {
        log.warn("Concurrent modification conflict: {}", ex.getClass().getSimpleName());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Concurrent modification conflict on shopping cart"
        );
        problemDetail.setTitle("Cart State Conflict");
        return problemDetail;
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, QueryTimeoutException.class})
    public ProblemDetail handleDatabaseUnavailable(RuntimeException ex) {
        log.error("Database unavailable", ex);
        return serviceUnavailableProblemDetail();
    }

    @ExceptionHandler(UncategorizedMongoDbException.class)
    public ProblemDetail handleUncategorizedMongo(UncategorizedMongoDbException ex) {
        Throwable cause = ex.getCause();
        log.error("Uncategorized MongoDB failure", ex);
        if (cause instanceof MongoExecutionTimeoutException
                || cause instanceof MongoTimeoutException
                || cause instanceof MongoSocketException) {
            return serviceUnavailableProblemDetail();
        }
        return internalErrorProblemDetail();
    }

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Validation Error");
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));
        if (detail.isBlank()) {
            detail = "Invalid request payload";
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle("Validation Error");
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed or invalid request payload"
        );
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName()
        );
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Missing required parameter: " + ex.getParameterName()
        );
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ProblemDetail handleBadRequest(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters");
        problemDetail.setTitle("Bad Request");
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedError(Exception ex) {
        log.error("Unexpected error", ex);
        return internalErrorProblemDetail();
    }

    private ProblemDetail serviceUnavailableProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Shopping cart storage is temporarily unavailable; please retry shortly"
        );
        problemDetail.setTitle("Service Temporarily Unavailable");
        return problemDetail;
    }

    private ProblemDetail internalErrorProblemDetail() {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        return problemDetail;
    }
}
