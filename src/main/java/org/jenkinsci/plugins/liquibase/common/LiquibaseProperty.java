package org.jenkinsci.plugins.liquibase.common;

public enum LiquibaseProperty {
    USERNAME,
    PASSWORD,
    CLASSPATH("classpath"),
    LABELS,
    DEFAULTS_FILE("defaultsFile"),
    CHANGELOG_FILE("changeLogFile"),
    CONTEXTS(),
    URL(),
    DEFAULT_SCHEMA_NAME("defaultSchemaName"),
    LOG_LEVEL("logLevel"),
    DEFAULT_CATALOG_NAME("defaultCatalogName"),
    DRIVER();


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
