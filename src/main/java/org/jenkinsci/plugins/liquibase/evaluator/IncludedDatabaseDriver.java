package org.jenkinsci.plugins.liquibase.evaluator;

/**
 * Describes database drivers that are included with the plugin.
 */
public class IncludedDatabaseDriver {
    private String displayName;
    private String driverClassName;


    public IncludedDatabaseDriver(String displayName, String driverClassName) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }
}
