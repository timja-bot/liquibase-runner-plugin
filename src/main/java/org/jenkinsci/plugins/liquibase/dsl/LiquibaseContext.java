package org.jenkinsci.plugins.liquibase.dsl;

import javaposse.jobdsl.dsl.Context;

import java.util.Map;

/**
 * One context is supplied for all liquibase execution types (be it update, rollback, etc).
 */
public class LiquibaseContext implements Context {

    protected String changeLogFile;
    protected String url;
    protected String contexts;
    protected String liquibasePropertiesPath;
    protected String labels;
    private Map<String, String> changeLogParameters;
    private String resourceDirectories;
    private String credentialsId;
    protected Integer rollbackCount = 0;
    private Integer rollbackLastHours;
    private String rollbackToTag ;
    private String rollbackToDate;
    protected boolean testRollbacks;
    private boolean dropAll;
    protected boolean tagOnSuccessfulBuild;
    private String outputDirectory;

    private String tag;

    private String commandArguments;

    void changeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    void url(String url) {
        this.url = url;
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

    void labels(String labels) {
        this.labels = labels;
    }

    void changeLogParameters(Map changeLogParameters) {
        this.changeLogParameters = changeLogParameters;
    }

    void resourceDirectories(String resourceDirectories) {
        this.resourceDirectories = resourceDirectories;
    }

    void rollbackCount(int rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }

    public String getUrl() {
        return url;
    }

    public String getContexts() {
        return contexts;
    }

    public String getLiquibasePropertiesPath() {
        return liquibasePropertiesPath;
    }

    public String getLabels() {
        return labels;
    }

    public Map getChangeLogParameters() {
        return changeLogParameters;
    }

    public String getResourceDirectories() {
        return resourceDirectories;
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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getCommandArguments() {
        return commandArguments;
    }

    public void setCommandArguments(String commandArguments) {
        this.commandArguments = commandArguments;
    }

    public String composeChangeLogString() {
        StringBuilder sb = new StringBuilder("");
        String result = null;
        if (changeLogParameters != null) {
            for (Map.Entry<String, String> entry : changeLogParameters.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            result = sb.substring(0, sb.length() - 1);
        } else {
            result = sb.toString();
        }


        return result;
    }
}
