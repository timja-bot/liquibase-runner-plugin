package org.jenkinsci.plugins.liquibase.builder;

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
    }
}
