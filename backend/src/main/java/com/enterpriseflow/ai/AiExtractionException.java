package com.enterpriseflow.ai;

/** A failed extraction, classified so callers can react without knowing provider-specific errors. */
public class AiExtractionException extends RuntimeException {

    public enum Reason {
        TIMEOUT("The AI service took too long to respond."),
        RATE_LIMITED("The AI service is busy. Please try again shortly."),
        PROVIDER_ERROR("The AI service returned an error."),
        INVALID_OUTPUT("The AI returned data that could not be used."),
        REFUSED("The AI service declined to process this document.");

        private final String userMessage;

        Reason(String userMessage) {
            this.userMessage = userMessage;
        }

        /** A message that is safe to show to users: it never contains provider internals. */
        public String userMessage() {
            return userMessage;
        }
    }

    private final Reason reason;

    public AiExtractionException(Reason reason, String message) {
        this(reason, message, null);
    }

    public AiExtractionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
