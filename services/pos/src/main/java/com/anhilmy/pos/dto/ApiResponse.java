package com.anhilmy.pos.dto;

import java.time.Instant;

/**
 * Standard response envelope for every API endpoint.
 *
 * <pre>
 * success: { "success": true,  "data": {...}, "timestamp": "..." }
 * failure: { "success": false, "error": { "code": "...", "message": "..." }, "timestamp": "..." }
 * </pre>
 */
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;
    private final ErrorDetail error;
    private final String timestamp;

    private ApiResponse(boolean success, T data, String message, ErrorDetail error) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.error = error;
        this.timestamp = Instant.now().toString();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, null, new ErrorDetail(code, message));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public ErrorDetail getError() {
        return error;
    }

    public String getTimestamp() {
        return timestamp;
    }

    /** Machine-readable error payload. */
    public static class ErrorDetail {
        private final String code;
        private final String message;

        public ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
