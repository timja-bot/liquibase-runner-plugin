package org.jenkinsci.plugins.liquibase.workflow;

import hudson.Extension;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class LiquibaseUpdateBuildStep extends AbstractStepImpl {
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
    private Boolean useIncludedDriver;
    private String credentialsId;
    protected boolean testRollbacks;
    private boolean dropAll;
    protected boolean tagOnSuccessfulBuild;

    @DataBoundConstructor
    public LiquibaseUpdateBuildStep(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }


    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(LiquibaseUpdateExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "liquibaseUpdate";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Evaluate liquibase changesets";
        }
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
    public void setUseIncludedDriver(Boolean useIncludedDriver) {
        this.useIncludedDriver = useIncludedDriver;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setTestRollbacks(boolean testRollbacks) {
        this.testRollbacks = testRollbacks;
    }

    @DataBoundSetter
    public void setDropAll(boolean dropAll) {
        this.dropAll = dropAll;
    }

    @DataBoundSetter
    public void setTagOnSuccessfulBuild(boolean tagOnSuccessfulBuild) {
        this.tagOnSuccessfulBuild = tagOnSuccessfulBuild;
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

    public Boolean getUseIncludedDriver() {
        return useIncludedDriver;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public boolean isTestRollbacks() {
        return testRollbacks;
    }

    public boolean isDropAll() {
        return dropAll;
    }

    public boolean isTagOnSuccessfulBuild() {
        return tagOnSuccessfulBuild;
    }
}
