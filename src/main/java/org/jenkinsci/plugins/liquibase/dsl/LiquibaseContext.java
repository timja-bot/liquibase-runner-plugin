package org.jenkinsci.plugins.liquibase.dsl;

import javaposse.jobdsl.dsl.Context;

import java.util.Map;

/**
 * One context is supplied for all liquibase execution types (be it update, rollback, etc).
 */
public class LiquibaseContext implements Context {

    protected String databaseEngine;
    protected String changeLogFile;
    protected String url;
    protected String defaultSchemaName;
    protected String contexts;
    protected String liquibasePropertiesPath;
    protected String classpath;
    protected String driverClassname;
    protected String labels;
    private Map<String, String> changeLogParameters;
    private String basePath;
    private Boolean useIncludedDriver;
    private String credentialsId;
    protected Integer rollbackCount = 0;
    private Integer rollbackLastHours;
    private String rollbackToTag ;
    private String rollbackToDate;
    protected boolean testRollbacks;
    private boolean dropAll;
    protected boolean tagOnSuccessfulBuild;
    private String outputDirectory;


    void databaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    void changeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    void url(String url) {
        this.url = url;
    }

    void defaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    void contexts(String contexts) {
        this.contexts = contexts;
    }

    void liquibasePropertiesPath(String liquibasePropertiesPath) {
        this.liquibasePropertiesPath = liquibasePropertiesPath;
    }

    void credentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }
    void classpath(String classpath) {
        this.classpath = classpath;
    }

    void driverClassname(String driverClassname) {
        this.driverClassname = driverClassname;
    }

    void labels(String labels) {
        this.labels = labels;
    }

    void changeLogParameters(Map changeLogParameters) {
        this.changeLogParameters = changeLogParameters;
    }

    void basePath(String basePath) {
        this.basePath = basePath;
    }

    void rollbackCount(int rollbackCount) {
        this.rollbackCount = rollbackCount;
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

    public Map getChangeLogParameters() {
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

    public String getRollbackToDate() {
        return rollbackToDate;
    }

    public void rollbackToDate(String rollbackToDate) {
        this.rollbackToDate = rollbackToDate;
    }

    public String getRollbackToTag() {
        return rollbackToTag;
    }

    public void rollbackToTag(String rollbackToTag) {
        this.rollbackToTag = rollbackToTag;
    }

    public Integer getRollbackLastHours() {
        return rollbackLastHours;
    }

    public void rollbackLastHours(Integer rollbackLastHours) {
        this.rollbackLastHours = rollbackLastHours;
    }

    public Integer getRollbackCount() {
        return rollbackCount;
    }

    public void setRollbackCount(int rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    public boolean isTagOnSuccessfulBuild() {
        return tagOnSuccessfulBuild;
    }

    public void tagOnSuccessfulBuild(boolean tagOnSuccessfulBuild) {
        this.tagOnSuccessfulBuild = tagOnSuccessfulBuild;
    }

    public boolean isDropAll() {
        return dropAll;
    }

    public void dropAll(boolean dropAll) {
        this.dropAll = dropAll;
    }

    public boolean isTestRollbacks() {
        return testRollbacks;
    }

    public void testRollbacks(boolean testRollbacks) {
        this.testRollbacks = testRollbacks;
    }

    public void outputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String composeChangeLogString() {
        StringBuilder sb = new StringBuilder("");
        String result = null;
        if (changeLogParameters != null) {
            for (String key : changeLogParameters.keySet()) {
                sb.append(key).append("=").append(changeLogParameters.get(key)).append("\n");
            }
            result = sb.substring(0, sb.length() - 1);
        } else {
            result = sb.toString();
        }


        return result;
    }
}
