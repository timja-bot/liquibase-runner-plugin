package org.jenkinsci.plugins.liquibase.builder;

public enum CliOption {
    USERNAME,
    PASSWORD,
    DEFAULTS_FILE("defaultsFile"), CHANGELOG_FILE("changeLogFile"), CONTEXTS(), URL(), DEFAULT_SCHEMA_NAME(
            "defaultSchemaName"), DATABASE_DRIVER_NAME("driver"), LOG_LEVEL("logLevel");


    private String cliOption;

    CliOption(String cliOption) {
        this.cliOption = cliOption;
    }

    CliOption() {
    }

    public String getCliOption() {
        String optionName;
        if (cliOption == null) {
            optionName = name().toLowerCase();
        } else {
            optionName = cliOption;
        }
        return optionName;
    }
}
