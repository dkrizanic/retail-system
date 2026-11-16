package com.retailsystem.cart.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoExecutionTimeoutException;
import com.retailsystem.cart.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapConcurrencyAndMongoErrorsToSanitizedProblemDetails() {
        ProblemDetail duplicateConflict = handler.handleConcurrencyConflict(new DuplicateKeyException("raw key"));
        assertThat(duplicateConflict.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(duplicateConflict.getDetail()).doesNotContain("raw key");

        ProblemDetail optimisticConflict = handler.handleConcurrencyConflict(new OptimisticLockingFailureException("raw version"));
        assertThat(optimisticConflict.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());

        UncategorizedMongoDbException mongoTimeout = new UncategorizedMongoDbException(
                "timed out",
                new MongoExecutionTimeoutException(50, "maxTimeMS exceeded")
        );
        ProblemDetail timeoutProblem = handler.handleUncategorizedMongo(mongoTimeout);
        assertThat(timeoutProblem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());

        ProblemDetail unexpectedProblem = handler.handleUnexpectedError(new RuntimeException("secret internal detail"));
        assertThat(unexpectedProblem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(unexpectedProblem.getDetail()).doesNotContain("secret internal detail");

        ProblemDetail malformedBody = handler.handleHttpMessageNotReadable(
                new HttpMessageNotReadableException("raw jackson detail", (HttpInputMessage) null)
        );
        assertThat(malformedBody.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(malformedBody.getDetail()).doesNotContain("raw jackson detail");

        ProblemDetail missingParam = handler.handleMissingRequestParameter(
                new MissingServletRequestParameterException("offerId", "String")
        );
        assertThat(missingParam.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(missingParam.getDetail()).contains("offerId");
    }
}
