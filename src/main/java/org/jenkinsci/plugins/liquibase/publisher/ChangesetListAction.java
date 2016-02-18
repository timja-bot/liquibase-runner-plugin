package org.jenkinsci.plugins.liquibase.publisher;

import hudson.model.Action;

import java.util.List;

import org.jenkinsci.plugins.liquibase.builder.ChangeSetAction;

import com.google.common.collect.Lists;

public class ChangesetListAction implements Action{

    private List<ChangeSetAction> changeSetActions = Lists.newArrayList();

    public List<ChangeSetAction> getChangeSetActions() {
        return changeSetActions;
    }

    public void addChangeset(ChangeSetAction changeSetAction) {
        changeSetActions.add(changeSetAction);
    }

    public void setChangeSetActions(List<ChangeSetAction> changeSetActions) {
        this.changeSetActions = changeSetActions;
    }

    public String getIconFileName() {
        return "/plugin/liquibase-runner/liquibase_icon24x24.png";
    }

    public String getDisplayName() {
        return "Changesets:";
    }

    public String getUrlName() {
        return "executedChangeSets";
    }
}
