package com.company.expensetracker.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Global exception handler that translates application exceptions into RFC 7807
 * Problem Details responses ({@code application/problem+json}).
 *
 * <p>Covers validation errors (400), method-not-allowed (405), data conflicts (409),
 * optimistic-lock retries (409), and a catch-all fallback (500).
 * All handlers set a typed {@code type} URI referencing the relevant HTTP status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean-validation failures on {@code @RequestBody} payloads.
     *
     * <p>Collects every field error and concatenates them into a single detail string.
     *
     * @param ex the validation exception raised by Spring MVC
     * @return a 400 Problem Detail with aggregated field error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://httpstatuses.io/400"));
        problem.setTitle("Validation Failed");
        return problem;
    }

    /**
     * Handles constraint violations raised at the service or repository layer
     * (e.g. {@code @Positive}, {@code @Size} on method parameters).
     *
     * @param ex the constraint violation exception
     * @return a 400 Problem Detail with aggregated constraint messages
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://httpstatuses.io/400"));
        problem.setTitle("Validation Failed");
        return problem;
    }

    /**
     * Handles unreadable or missing request bodies (malformed JSON, wrong content-type).
     *
     * @param ex the message-not-readable exception
     * @return a 400 Problem Detail with a generic malformed-body message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed or missing request body");
        problem.setType(URI.create("https://httpstatuses.io/400"));
        problem.setTitle("Bad Request");
        return problem;
    }

    /**
     * Handles requests that use an HTTP method not mapped for the target resource.
     *
     * @param ex the method-not-supported exception containing the disallowed method name
     * @return a 405 Problem Detail indicating which method was rejected
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED,
                "Method " + ex.getMethod() + " not allowed");
        problem.setType(URI.create("https://httpstatuses.io/405"));
        problem.setTitle("Method Not Allowed");
        return problem;
    }

    /**
     * Handles database constraint violations (unique-key, foreign-key, not-null violations).
     *
     * <p>Typically arises when a duplicate email or category name is submitted.
     * The raw database message is not exposed to the client.
     *
     * @param ex the data integrity exception
     * @return a 409 Problem Detail with a generic conflict message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data conflict: resource already exists or constraint violated");
        problem.setType(URI.create("https://httpstatuses.io/409"));
        problem.setTitle("Conflict");
        return problem;
    }

    /**
     * Handles optimistic-locking conflicts that arise when two concurrent requests
     * attempt to modify the same entity version.
     *
     * <p>The client should retry the request after re-fetching the resource.
     *
     * @param ex the optimistic-locking failure exception
     * @return a 409 Problem Detail advising the client to retry
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Resource was modified by another request. Please retry.");
        problem.setType(URI.create("https://httpstatuses.io/409"));
        problem.setTitle("Conflict");
        return problem;
    }

    /**
     * Handles {@link ResponseStatusException} thrown by service or controller code.
     *
     * <p>The HTTP status and reason are passed through directly, allowing services
     * to signal 400, 401, 403, 404, and 409 errors with a descriptive message.
     *
     * @param ex the response-status exception carrying the desired status and reason
     * @return a Problem Detail whose status and title mirror the exception
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason());
        problem.setType(URI.create("https://httpstatuses.io/" + ex.getStatusCode().value()));
        problem.setTitle(HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase());
        return problem;
    }

    /**
     * Handles illegal-argument errors raised by application code (e.g. unknown enum value,
     * invalid UUID format passed to a service).
     *
     * @param ex the illegal-argument exception with a descriptive message
     * @return a 400 Problem Detail forwarding the exception message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://httpstatuses.io/400"));
        problem.setTitle("Bad Request");
        return problem;
    }

    /**
     * Catch-all handler for any unrecognised exception type.
     *
     * <p>Logs the full stack trace at ERROR level and returns a 500 response.
     * The internal exception message is never forwarded to avoid leaking sensitive details.
     *
     * @param ex the unhandled exception
     * @return a 500 Problem Detail with a generic error message
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://httpstatuses.io/500"));
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
