package org.jenkinsci.plugins.liquibase.workflow;

import hudson.Extension;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class RollbackStep extends AbstractLiquibaseStep {
    private String rollbackType;
    protected int rollbackCount;
    private String rollbackLastHours;
    private String rollbackToTag;
    private String rollbackToDate;

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
    public void setRollbackType(String rollbackType) {
        this.rollbackType = rollbackType;
    }

    @DataBoundSetter
    public void setRollbackCount(int rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    @DataBoundSetter
    public void setRollbackLastHours(String rollbackLastHours) {
        this.rollbackLastHours = rollbackLastHours;
    }

    @DataBoundSetter
    public void setRollbackToTag(String rollbackToTag) {
        this.rollbackToTag = rollbackToTag;
    }

    @DataBoundSetter
    public void setRollbackToDate(String rollbackToDate) {
        this.rollbackToDate = rollbackToDate;
    }

    public String getRollbackType() {
        return rollbackType;
    }

    public int getRollbackCount() {
        return rollbackCount;
    }

    public String getRollbackLastHours() {
        return rollbackLastHours;
    }

    public String getRollbackToTag() {
        return rollbackToTag;
    }

    public String getRollbackToDate() {
        return rollbackToDate;
    }
}
