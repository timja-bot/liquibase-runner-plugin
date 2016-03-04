package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import liquibase.changelog.ChangeSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
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
        Collection<ChangeSetDetail> filtered = Collections2.filter(changeSetDetails, new Predicate<ChangeSetDetail>() {
            @Override
            public boolean apply(@Nullable ChangeSetDetail changeSetDetail) {

                boolean include = false;
                if (changeSetDetail != null) {
                    include = !changeSetDetail.isSuccessfullyExecuted();

                }
                return include;
            }
        });
        return new ArrayList<ChangeSetDetail>(filtered);
    }

    public void setBuild(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public void addChangeset(ChangeSet changeSet) {
        ChangeSetDetail changeSetDetail = ChangeSetDetail.create(changeSet);
        addChangeSetDetail(changeSetDetail);

    }

    public List<ChangeSetDetail> getSuccessfulChangeSets() {
        Collection<ChangeSetDetail> successful = Collections2.filter(changeSetDetails, new Predicate<ChangeSetDetail>() {
            @Override
            public boolean apply(@Nullable ChangeSetDetail changeSetDetail) {
                boolean include = false;
                if (changeSetDetail != null) {
                    include = changeSetDetail.isSuccessfullyExecuted();
                }
                return include;
            }
        });
        return new ArrayList<ChangeSetDetail>(successful);
    }

    protected void addChangeSetDetail(ChangeSetDetail changeSetDetail) {
        changeSetDetail.setParent(this);
        // since testing rollbacks executes changesets twice, only add changeset if it isn't already present.
        if (!changeSetDetails.contains(changeSetDetail)) {
            changeSetDetails.add(changeSetDetail);
        }
    }

    public ChangeSetDetail getChangeset(final String id) {
        ChangeSetDetail found = null;
        for (ChangeSetDetail changeSetDetail : changeSetDetails) {
            if (changeSetDetail.getId().equals(id)) {
                found=changeSetDetail;
                break;
            }
        }
        return found;
    }
}
