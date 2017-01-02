package org.jenkinsci.plugins.liquibase.workflow;

import hudson.Extension;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class LiquibaseUpdateBuildStep extends AbstractLiquibaseStep {

    protected boolean testRollbacks;
    private boolean dropAll;
    protected boolean tagOnSuccessfulBuild;

    @DataBoundConstructor
    public LiquibaseUpdateBuildStep(String changeLogFile) {
        super(changeLogFile);
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
