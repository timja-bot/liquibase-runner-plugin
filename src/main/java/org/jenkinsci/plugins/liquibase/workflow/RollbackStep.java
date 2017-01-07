package org.jenkinsci.plugins.liquibase.workflow;

import hudson.Extension;
import hudson.Util;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class RollbackStep extends AbstractLiquibaseStep {
    protected int rollbackCount = 0;
    private String rollbackLastHours = null;
    private String rollbackToTag = null;
    private String rollbackToDate = null;

    @DataBoundConstructor
    public RollbackStep(String changeLogFile) {
        super(changeLogFile);
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(RollbackStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "liquibaseRollback";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Roll back liquibase changes";
        }
    }


    @DataBoundSetter
    public void setRollbackCount(int rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    @DataBoundSetter
    public void setRollbackLastHours(String rollbackLastHours) {
        this.rollbackLastHours = Util.fixEmptyAndTrim(rollbackLastHours);
    }

    @DataBoundSetter
    public void setRollbackToTag(String rollbackToTag) {
        this.rollbackToTag = Util.fixEmptyAndTrim(rollbackToTag);
    }

    @DataBoundSetter
    public void setRollbackToDate(String rollbackToDate) {
        this.rollbackToDate = Util.fixEmptyAndTrim(rollbackToDate);
    }

    public int getRollbackCount() {
        return rollbackCount;
    }

    @CheckForNull
    public String getRollbackLastHours() {
        return rollbackLastHours;
    }

    @CheckForNull
    public String getRollbackToTag() {
        return rollbackToTag;
    }

    @CheckForNull
    public String getRollbackToDate() {
        return rollbackToDate;
    }
}
