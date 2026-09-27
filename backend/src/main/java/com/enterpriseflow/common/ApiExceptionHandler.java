package com.enterpriseflow.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Returns every error as an RFC 9457 Problem Details response.
 *
 * <p>Spring's own exceptions (validation, missing resources, bad requests) are handled by the parent
 * class. Anything unexpected becomes a generic 500 that never exposes stack traces or internal details.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Uploads far over the limit are stopped by the servlet container before the application sees them.
     * Answer them in the same shape as the application's own "file too large" response.
     */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "The file exceeds the maximum upload size.");
        problem.setType(URI.create("urn:enterpriseflow:problem:file-too-large"));
        problem.setTitle("File too large");
        return handleExceptionInternal(exception, problem, headers, HttpStatus.PAYLOAD_TOO_LARGE, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
