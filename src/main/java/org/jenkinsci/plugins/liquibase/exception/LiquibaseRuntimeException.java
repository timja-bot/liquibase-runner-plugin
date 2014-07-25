package org.jenkinsci.plugins.liquibase.exception;

/**
 * Translates any liquibase checked exceptions as runtime exceptions.
 */
public class LiquibaseRuntimeException extends RuntimeException {
    public LiquibaseRuntimeException() {
        super();
    }

    public LiquibaseRuntimeException(String message) {
        super(message);
    }

    public LiquibaseRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiquibaseRuntimeException(Throwable cause) {
        super(cause);
    }

    protected LiquibaseRuntimeException(String message,
                                        Throwable cause,
                                        boolean enableSuppression,
                                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public LiquibaseRuntimeException(String s, Exception e) {
        super(s, e);
    }
}
