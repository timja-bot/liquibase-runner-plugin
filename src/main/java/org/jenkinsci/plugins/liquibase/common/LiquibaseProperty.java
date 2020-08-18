package org.jenkinsci.plugins.liquibase.common;

public enum LiquibaseProperty {
    USERNAME,
    PASSWORD,
    LABELS,
    DEFAULTS_FILE("defaultsFile"),
    CHANGELOG_FILE("changeLogFile"),
    CONTEXTS(),
    URL(),
    LOG_LEVEL("logLevel");


    private String cliOption;

    LiquibaseProperty(String cliOption) {
        this.cliOption = cliOption;
    }

    LiquibaseProperty() {
    }

    public String propertyName() {
        String optionName;
        if (cliOption == null) {
            optionName = name().toLowerCase();
        } else {
            optionName = cliOption;
        }
        return optionName;
    }
}
