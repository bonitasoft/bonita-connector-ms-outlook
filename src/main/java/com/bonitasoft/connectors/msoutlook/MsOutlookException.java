package com.bonitasoft.connectors.msoutlook;

/**
 * Typed exception for MS Outlook connector.
 */
public class MsOutlookException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public MsOutlookException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public MsOutlookException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public MsOutlookException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public MsOutlookException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
