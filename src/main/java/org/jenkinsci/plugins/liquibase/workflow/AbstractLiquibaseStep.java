package org.jenkinsci.plugins.liquibase.workflow;

import hudson.Util;

import java.util.List;

import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundSetter;

public abstract class AbstractLiquibaseStep extends AbstractStepImpl {
    protected final String changeLogFile;
    protected String databaseEngine = null;
    protected String url = null;
    protected String defaultSchemaName = null;
    protected String contexts = null;
    protected String liquibasePropertiesPath = null;
    protected String classpath = null;
    protected String driverClassname = null;
    protected String labels = null;
    private String basePath = null;
    private List<String> changeLogParameterList = null;
    private String changeLogParameters = null;

    private String credentialsId;

    public AbstractLiquibaseStep(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    @DataBoundSetter
    public void setChangeLogParameterList(List<String> changeLogParameterList) {
        this.changeLogParameterList = changeLogParameterList;
    }

    @DataBoundSetter
    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = Util.fixEmptyAndTrim(Util.fixEmptyAndTrim(databaseEngine));
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = Util.fixEmptyAndTrim(url);
    }

    @DataBoundSetter
    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = Util.fixEmptyAndTrim(defaultSchemaName);
    }

    @DataBoundSetter
    public void setContexts(String contexts) {
        this.contexts = Util.fixEmptyAndTrim(contexts);
    }

    @DataBoundSetter
    public void setLiquibasePropertiesPath(String liquibasePropertiesPath) {
        this.liquibasePropertiesPath = Util.fixEmptyAndTrim(liquibasePropertiesPath);
    }

    @DataBoundSetter
    public void setClasspath(String classpath) {
        this.classpath = Util.fixEmptyAndTrim(classpath);
    }

    @DataBoundSetter
    public void setDriverClassname(String driverClassname) {
        this.driverClassname = Util.fixEmptyAndTrim(driverClassname);
    }

    @DataBoundSetter
    public void setLabels(String labels) {
        this.labels = Util.fixEmptyAndTrim(labels);
    }

    @DataBoundSetter
    public void setBasePath(String basePath) {
        this.basePath = Util.fixEmptyAndTrim(basePath);
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
    }

    @DataBoundSetter
    public void setChangeLogParameters(String changeLogParameters) {
        this.changeLogParameters = Util.fixEmptyAndTrim(changeLogParameters);
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

    public String getBasePath() {
        return basePath;
    }

    public List<String> getChangeLogParameterList() {
        return changeLogParameterList;
    }

    public String getChangeLogParameters() {
        return changeLogParameters;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

}
