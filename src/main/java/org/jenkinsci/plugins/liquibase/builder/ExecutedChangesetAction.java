package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import liquibase.changelog.ChangeSet;
import liquibase.sql.Sql;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Supplies information any executed changesets to a particular build.
 */
public class ExecutedChangesetAction implements Action {

    private AbstractBuild<?,?> build;

    Map<ChangeSet, ChangeSetDetail> changeSetDetails = Maps.newHashMap();

    private List<ChangeSet> failed = Lists.newArrayList();

    private List<ChangeSetDetail> changeSetDetailList = Lists.newArrayList();

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

    public void addChangeset(ChangeSet changeSet) {
        ChangeSetDetail changeSetDetail = ChangeSetDetail.create(changeSet);
        if (!changeSetDetails.containsKey(changeSet)) {
            changeSetDetails.put(changeSet, changeSetDetail);
        }

    }

    public Map<ChangeSet, ChangeSetDetail> getChangeSetDetails() {
        return changeSetDetails;
    }

    public void addFailed(ChangeSet changeSet) {
        failed.add(changeSet);
    }

    public void addChangesetWithSql(ChangeSet changeSet, Sql[] sqls) {
        ChangeSetDetail changeSetDetail = ChangeSetDetail.createWithSql(changeSet, sqls);
        changeSetDetails.put(changeSet, changeSetDetail);

    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public List<ChangeSetDetail> getChangeSetDetailList() {
        return changeSetDetailList;
    }

    public void setBuild(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public void addChangesetWithSql(ChangeSet changeSet, List<Sql> statementSqls) {
        ChangeSetDetail changeSetDetail = ChangeSetDetail.createWithSql(changeSet, statementSqls);
        changeSetDetailList.add(changeSetDetail);

        changeSetDetails.put(changeSet, changeSetDetail);

    }
}
