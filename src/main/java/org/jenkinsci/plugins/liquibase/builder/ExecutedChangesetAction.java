package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import liquibase.changelog.ChangeSet;
import liquibase.sql.Sql;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Supplies information any executed changesets to a particular build.
 */
public class ExecutedChangesetAction implements Action {

    private AbstractBuild<?,?> build;

    private List<ChangeSet> failed = Lists.newArrayList();

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

    public void addFailed(ChangeSet changeSet) {
        failed.add(changeSet);
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
    public List<ChangeSetDetail> getChangeSetDetails() {
        return changeSetDetails;
    }

    public void setBuild(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public void addChangesetWithSql(ChangeSet changeSet, List<Sql> statementSqls) {
        ChangeSetDetail changeSetDetail = ChangeSetDetail.createWithSql(changeSet, statementSqls);
        // since testing rollbacks executes changesets twice, only add changeset if it isn't already contained.
        if (!changeSetDetails.contains(changeSetDetail)) {
            changeSetDetails.add(changeSetDetail);
        }
    }
}
