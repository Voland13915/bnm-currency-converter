package md.bnm.converter.service;

import java.util.Objects;

/**
 * Exception thrown when loading or parsing exchange rates fails.
 * Carries an error type identifying the cause (network, timeout, server error, malformed response, etc.)
 * and a user-facing explanation in Russian.
 */
public class RateLoadException extends Exception {

    private final RateLoadErrorType errorType;

    public RateLoadException(RateLoadErrorType errorType, String message, Throwable cause) {
        super(message != null ? message : Objects.requireNonNull(errorType, "errorType must not be null").getDefaultMessage(), cause);
        this.errorType = Objects.requireNonNull(errorType, "errorType must not be null");
    }

    public RateLoadException(RateLoadErrorType errorType, String message) {
        this(errorType, message, null);
    }

    public RateLoadException(RateLoadErrorType errorType) {
        this(errorType, errorType.getDefaultMessage(), null);
    }

    public RateLoadException(RateLoadErrorType errorType, Throwable cause) {
        this(errorType, errorType.getDefaultMessage(), cause);
    }

    public RateLoadErrorType getErrorType() {
        return errorType;
    }

    public String getUserMessage() {
        return getMessage();
    }
}
