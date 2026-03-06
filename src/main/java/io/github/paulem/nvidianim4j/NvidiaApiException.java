package io.github.paulem.nvidianim4j;

/**
 * Thrown when the NVIDIA NIM API returns a non-successful HTTP status or when a
 * network-level error occurs.
 */
public class NvidiaApiException extends RuntimeException {

    private final int statusCode;

    /**
     * Creates an exception for an HTTP error response.
     *
     * @param statusCode the HTTP status code
     * @param body       the error response body
     */
    public NvidiaApiException(int statusCode, String body) {
        super("NVIDIA NIM API error – HTTP " + statusCode + ": " + body);
        this.statusCode = statusCode;
    }

    /**
     * Creates an exception for a network-level error.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public NvidiaApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    /**
     * Returns the HTTP status code, or {@code -1} for network-level errors.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
