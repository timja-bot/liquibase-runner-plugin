package org.jenkinsci.plugins.liquibase.common;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;

import java.io.IOException;
import java.util.Properties;

/**
 * Common configuration for each type of liquibase builder.
 */
public abstract class AbstractLiquibaseBuildStep extends Builder {
    /**
     * Root changeset file.
     */
    protected String changeLogFile;
    /**
     * Username with which to connect to database.
     */
    protected String username;
    /**
     * Password with which to connect to database.
     */
    protected String password;
    /**
     * JDBC database connection URL.
     */
    protected String url;
    protected String defaultSchemaName;
    /**
     * Contexts to activate during execution.
     */
    protected String contexts;
    protected boolean testRollbacks;
    protected String liquibasePropertiesPath;
    private String driverName;

    public AbstractLiquibaseBuildStep(String url,
                                      String password,
                                      String changeLogFile,
                                      String username,
                                      String defaultSchemaName,
                                      String liquibasePropertiesPath,
                                      boolean testRollbacks,
                                      String contexts) {
        this.url = url;
        this.password = password;
        this.changeLogFile = changeLogFile;
        this.username = username;
        this.defaultSchemaName = defaultSchemaName;
        this.liquibasePropertiesPath = liquibasePropertiesPath;
        this.testRollbacks = testRollbacks;
        this.contexts = contexts;

    }

    protected abstract boolean doPerform(AbstractBuild<?, ?> build,
                                         Launcher launcher,
                                         BuildListener listener,
                                         Properties configProperties) throws InterruptedException, IOException;

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        Properties configProperties = PropertiesParser.createConfigProperties(this);
        return doPerform(build, launcher, listener, configProperties);
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }

    public String getContexts() {
        return contexts;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    public String getLiquibasePropertiesPath() {
        return liquibasePropertiesPath;
    }

    public void setLiquibasePropertiesPath(String liquibasePropertiesPath) {
        this.liquibasePropertiesPath = liquibasePropertiesPath;
    }

    public void setChangeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    public void setContexts(String contexts) {
        this.contexts = contexts;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
}
