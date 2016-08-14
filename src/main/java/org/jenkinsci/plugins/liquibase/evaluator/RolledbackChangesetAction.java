package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.model.AbstractBuild;
import hudson.model.Action;

import java.util.List;

import com.google.common.collect.Lists;

public class RolledbackChangesetAction implements Action  {

    private AbstractBuild<?, ?> build;

    List<ChangeSetDetail> rolledbackChangesets = Lists.newArrayList();
    private boolean rollbacksExpected;


    public RolledbackChangesetAction() {
    }

    public RolledbackChangesetAction(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public void addRollback(ChangeSetDetail changeSetDetail) {
        rolledbackChangesets.add(changeSetDetail);
    }

    public List<ChangeSetDetail> getRolledbackChangesets() {
        return rolledbackChangesets;
    }

    public boolean isRollbacksExpected() {
        return rollbacksExpected;
    }

    public void setRollbacksExpected(boolean rollbacksExpected) {
        this.rollbacksExpected = rollbacksExpected;
    }

    public boolean isShouldSummarize() {
        return !rolledbackChangesets.isEmpty() || rollbacksExpected;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/liquibase-runner/undo_48x48.png";
    }

    @Override
    public String getDisplayName() {
        return "Rolled Back Changesets";
    }

    @Override
    public String getUrlName() {
        return "rolledbackChangesets";
    }
}
