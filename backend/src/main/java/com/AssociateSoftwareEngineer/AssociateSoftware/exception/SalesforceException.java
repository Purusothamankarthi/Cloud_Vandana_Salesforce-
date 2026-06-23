package com.AssociateSoftwareEngineer.AssociateSoftware.exception;

/**
 * Custom exception for Salesforce API errors.
 */
public class SalesforceException extends RuntimeException {

    private final int statusCode;

    public SalesforceException(String message) {
        super(message);
        this.statusCode = 500;
    }

    public SalesforceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public SalesforceException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
