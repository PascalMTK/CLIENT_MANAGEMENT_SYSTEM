package com.clientfiles.core;

/** A failure whose message is written for the person using the application. */
public class AppException extends RuntimeException {
    private final boolean connectionProblem;

    public AppException(String message) {
        this(message, null, false);
    }

    public AppException(String message, Throwable cause, boolean connectionProblem) {
        super(message, cause);
        this.connectionProblem = connectionProblem;
    }

    public boolean isConnectionProblem() {
        return connectionProblem;
    }
}
