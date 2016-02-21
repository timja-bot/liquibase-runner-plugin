package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import liquibase.changelog.ChangeSet;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

/**
 * Supplies information about executed changesets to a particular build.
 */
public class ExecutedChangesetAction implements Action {

    private AbstractBuild<?, ?> build;

    private List<ChangeSetDetail> changeSetDetails = Lists.newArrayList();

    public ExecutedChangesetAction() {
    }

    public ExecutedChangesetAction(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public String getIconFileName() {
        return "/plugin/liquibase-runner/liquibase_icon24x24.png";
    }

    public String getDisplayName() {
        return "Changesets";
    }

    public String getUrlName() {
        return "executedChangeSets";
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public List<ChangeSetDetail> getChangeSetDetails() {
        return changeSetDetails;
    }

    public List<ChangeSetDetail> getFailedChangeSets() {
        return changeSetDetails.stream().filter(changeSetDetail -> !changeSetDetail.isSuccessfullyExecuted())
                               .collect(Collectors.toList());
    }

    public void setBuild(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public void addChangeset(ChangeSet changeSet) {
        addChangeSetDetail(new ChangeSetDetail(changeSet));

    }

    public List<ChangeSetDetail> getSuccessfulChangeSets() {
        return changeSetDetails.stream().filter(changeSetDetail -> changeSetDetail.isSuccessfullyExecuted()).collect(
                Collectors.toList());
    }

    protected void addChangeSetDetail(ChangeSetDetail changeSetDetail) {
        // since testing rollbacks executes changesets twice, only add changeset if it isn't already present.
        if (!changeSetDetails.contains(changeSetDetail)) {
            changeSetDetails.add(changeSetDetail);
        }
    }
}
