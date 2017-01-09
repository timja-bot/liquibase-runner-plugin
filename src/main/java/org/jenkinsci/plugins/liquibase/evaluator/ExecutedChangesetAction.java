package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Supplies information about executed changesets to a particular build.
 */
public class ExecutedChangesetAction implements Action {

    private Run<?, ?> build;

    private List<ChangeSetDetail> changeSetDetails = Lists.newArrayList();

    private List<ChangeSetDetail> rolledBackChangesets = Lists.newArrayList();

    private boolean noExecutionsExpected;

    private String appliedTag;

    private boolean rollbacksTested;
    @Deprecated
    private transient Boolean rollbackOnly;

    public ExecutedChangesetAction() {
    }

    public ExecutedChangesetAction(Run<?, ?> build) {
        this.build = build;
    }

    protected Object readResolve() {
        if (rollbackOnly != null) {
            noExecutionsExpected = rollbackOnly;
        }
        return this;

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

    public Run<?, ?> getBuild() {
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

    public boolean areErrorsPresent() {
        boolean exceptionMessagesExist = false;
        for (ChangeSetDetail changeSetDetail : changeSetDetails) {
            exceptionMessagesExist = changeSetDetail.hasExceptionMessage();
            if (exceptionMessagesExist) {
                break;
            }
        }
        return exceptionMessagesExist;
    }

    public List<ChangeSetDetail> getSuccessfulChangeSets() {
        return filterChangeSetDetails(new Predicate<ChangeSetDetail>() {
            @Override
            public boolean apply(@Nullable ChangeSetDetail changeSetDetail) {
                boolean include = false;
                if (changeSetDetail != null) {
                    include = changeSetDetail.isSuccessfullyExecuted();
                }
                return include;
            }
        });
    }

    private List<ChangeSetDetail> filterChangeSetDetails(Predicate<ChangeSetDetail> predicate) {
        Collection<ChangeSetDetail> successful = Collections2.filter(changeSetDetails,
                predicate);
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

    public boolean isTagApplied() {
        return !Strings.isNullOrEmpty(appliedTag);
    }
    public String getAppliedTag() {
        return appliedTag;
    }

    public void setAppliedTag(String appliedTag) {
        this.appliedTag = appliedTag;
    }

    public void markChangesetAsRolledBack(String changeSetId) {
        for (ChangeSetDetail changeSetDetail : changeSetDetails) {
            if (changeSetDetail.getId().equals(changeSetId)) {
                changeSetDetail.setRolledBack(true);
            }
        }
    }

    public boolean hasChangesetWithId(String changesetId) {
        boolean result = false;
        for (ChangeSetDetail changeSetDetail : changeSetDetails) {
            if (changeSetDetail.getId().equals(changesetId)) {
                result=true;
                break;
            }
        }
        return result;
    }
    public boolean isRollbacksTested() {
        return rollbacksTested;
    }

    public void setRollbacksTested(boolean rollbacksTested) {
        this.rollbacksTested = rollbacksTested;
    }

    public void addRolledBackChangesetDetail(ChangeSetDetail changeSetDetail) {
        rolledBackChangesets.add(changeSetDetail);
    }
    public void setChangeSetDetails(List<ChangeSetDetail> changeSetDetails) {
        this.changeSetDetails = changeSetDetails;
    }

    public List<ChangeSetDetail> getRolledBackChangesets() {
        return rolledBackChangesets;
    }

    public void setRolledBackChangesets(List<ChangeSetDetail> rolledBackChangesets) {
        this.rolledBackChangesets = rolledBackChangesets;
    }


    public void setNoExecutionsExpected(boolean noExecutionsExpected) {
        this.noExecutionsExpected = noExecutionsExpected;
    }

    public boolean isNoExecutionsExpected() {
        return noExecutionsExpected;
    }

    @Deprecated
    public void setRollbackOnly(boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }

    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
}
