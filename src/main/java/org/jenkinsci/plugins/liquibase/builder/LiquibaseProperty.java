package org.jenkinsci.plugins.liquibase.builder;

public enum LiquibaseProperty {
    USERNAME,
    PASSWORD,
    DEFAULTS_FILE("defaultsFile"), CHANGELOG_FILE("changeLogFile"), CONTEXTS(), URL(), DEFAULT_SCHEMA_NAME(
            "defaultSchemaName"), DATABASE_DRIVER_NAME("driver"), LOG_LEVEL("logLevel");


    private String cliOption;

    LiquibaseProperty(String cliOption) {
        this.cliOption = cliOption;
    }

    LiquibaseProperty() {
    }

    public String getOptionName() {
        String optionName;
        if (cliOption == null) {
            optionName = name().toLowerCase();
        } else {
            optionName = cliOption;
        }
        return optionName;
    }
}
