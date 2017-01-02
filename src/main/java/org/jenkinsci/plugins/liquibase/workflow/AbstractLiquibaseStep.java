package org.jenkinsci.plugins.liquibase.workflow;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundSetter;

public class AbstractLiquibaseStep extends AbstractStepImpl {
    protected String databaseEngine;
    protected String changeLogFile;
    protected String url;
    protected String defaultSchemaName;
    protected String contexts;
    protected String liquibasePropertiesPath;
    protected String classpath;
    protected String driverClassname;
    protected String labels;
    private String changeLogParameters;
    private String basePath;

    @CheckForNull
    private String credentialsId;

    public AbstractLiquibaseStep(
            String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    @DataBoundSetter
    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    @DataBoundSetter
    public void setChangeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    @DataBoundSetter
    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    @DataBoundSetter
    public void setContexts(String contexts) {
        this.contexts = contexts;
    }

    @DataBoundSetter
    public void setLiquibasePropertiesPath(String liquibasePropertiesPath) {
        this.liquibasePropertiesPath = liquibasePropertiesPath;
    }

    @DataBoundSetter
    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    @DataBoundSetter
    public void setDriverClassname(String driverClassname) {
        this.driverClassname = driverClassname;
    }

    @DataBoundSetter
    public void setLabels(String labels) {
        this.labels = labels;
    }

    @DataBoundSetter
    public void setChangeLogParameters(String changeLogParameters) {
        this.changeLogParameters = changeLogParameters;
    }

    @DataBoundSetter
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }

    public String getUrl() {
        return url;
    }

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    public String getContexts() {
        return contexts;
    }

    public String getLiquibasePropertiesPath() {
        return liquibasePropertiesPath;
    }

    public String getClasspath() {
        return classpath;
    }

    public String getDriverClassname() {
        return driverClassname;
    }

    public String getLabels() {
        return labels;
    }

    public String getChangeLogParameters() {
        return changeLogParameters;
    }

    public String getBasePath() {
        return basePath;
    }

    @Nullable
    public String getCredentialsId() {
        return credentialsId;
    }


}
