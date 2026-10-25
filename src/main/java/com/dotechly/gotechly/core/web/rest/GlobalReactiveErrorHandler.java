package com.dotechly.gotechly.core.web.rest;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.netty.channel.AbortedException;

import java.net.ConnectException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Global safety-net error handler for all endpoints.
 * Catches timeout and connection exceptions that escape plugin-level handling.
 * Returns structured JSON error responses instead of generic Spring 500 errors.
 *
 * Defense layers:
 *   1. Plugin-level: MpesaValidationHandler.onErrorResume() handles most errors
 *   2. Global (this class): catches anything that slips through
 */
@RestControllerAdvice
public class GlobalReactiveErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalReactiveErrorHandler.class);

    /**
     * WebClient wraps Netty exceptions in WebClientRequestException.
     * Unwrap and categorize the root cause.
     */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientRequest(WebClientRequestException ex) {
        Throwable cause = unwrap(ex);
        LOG.error("Global handler: WebClientRequestException (cause: {}) - {}",
                cause.getClass().getSimpleName(), cause.getMessage(), ex);
        return categorize(cause);
    }

    @ExceptionHandler(ConnectTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleConnectTimeout(ConnectTimeoutException ex) {
        LOG.error("Global handler: TCP connect timeout - {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "CONNECTION_TIMEOUT",
                "Connection to upstream service timed out");
    }

    @ExceptionHandler(ReadTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleReadTimeout(ReadTimeoutException ex) {
        LOG.error("Global handler: Read timeout - {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "READ_TIMEOUT",
                "Upstream service stopped responding");
    }

    @ExceptionHandler(WriteTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleWriteTimeout(WriteTimeoutException ex) {
        LOG.error("Global handler: Write timeout - {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "WRITE_TIMEOUT",
                "Failed to send request to upstream service");
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeout(TimeoutException ex) {
        LOG.error("Global handler: Response timeout - {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "RESPONSE_TIMEOUT",
                "Upstream service response timed out");
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<Map<String, Object>> handleConnectException(ConnectException ex) {
        LOG.error("Global handler: Connection refused - {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_GATEWAY, "CONNECTION_REFUSED",
                "Upstream service is unreachable");
    }

    @ExceptionHandler(AbortedException.class)
    public ResponseEntity<Map<String, Object>> handleAborted(AbortedException ex) {
        LOG.error("Global handler: Connection aborted - {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_GATEWAY, "CONNECTION_ABORTED",
                "Connection to upstream service was aborted");
    }

    /**
     * Catch-all for plugin and service-layer runtime exceptions that escape
     * the reactive pipeline. Without this, the error dispatches to /error, where
     * InternalApiKeyFilter (OncePerRequestFilter) does not re-run, leaving an empty
     * SecurityContext — Spring Security then responds 403 instead of a useful body.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        LOG.error("Global handler: unhandled {} - {}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "PROCESSING_ERROR", message);
    }

    /**
     * Categorize an unwrapped exception into the appropriate HTTP response.
     */
    private ResponseEntity<Map<String, Object>> categorize(Throwable cause) {
        if (cause instanceof ConnectTimeoutException) {
            return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "CONNECTION_TIMEOUT",
                    "Connection to upstream service timed out");
        } else if (cause instanceof ReadTimeoutException) {
            return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "READ_TIMEOUT",
                    "Upstream service stopped responding");
        } else if (cause instanceof WriteTimeoutException) {
            return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "WRITE_TIMEOUT",
                    "Failed to send request to upstream service");
        } else if (cause instanceof TimeoutException) {
            return buildResponse(HttpStatus.GATEWAY_TIMEOUT, "RESPONSE_TIMEOUT",
                    "Upstream service response timed out");
        } else if (cause instanceof ConnectException) {
            return buildResponse(HttpStatus.BAD_GATEWAY, "CONNECTION_REFUSED",
                    "Upstream service is unreachable");
        } else {
            return buildResponse(HttpStatus.BAD_GATEWAY, "UPSTREAM_ERROR",
                    "Upstream service error: " + cause.getMessage());
        }
    }

    /**
     * Unwrap nested exceptions to find the root cause.
     */
    private Throwable unwrap(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "errorCode", errorCode,
                "message", message
        );
        return ResponseEntity.status(status).body(body);
    }
}
