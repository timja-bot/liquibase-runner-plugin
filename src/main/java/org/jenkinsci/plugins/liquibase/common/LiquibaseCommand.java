package org.jenkinsci.plugins.liquibase.common;

public enum LiquibaseCommand {
    UPDATE("update"),
    UPDATE_TESTING_ROLLBACKS("updateTestingRollback"), ROLLBACK("rollback");

    private final String command;

    LiquibaseCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
    public boolean isCommand(String command) {
        return this.command.equals(command);
    }
}
