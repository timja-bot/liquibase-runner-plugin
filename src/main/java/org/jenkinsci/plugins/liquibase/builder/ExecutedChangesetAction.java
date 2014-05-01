package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.Action;
import liquibase.changelog.ChangeSet;

import java.util.List;

import com.google.common.collect.Lists;

public class ExecutedChangesetAction implements Action {

    List<ChangeSet> changeSets = Lists.newArrayList();
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return "Executed ChangeSet";
    }

    public String getUrlName() {
        return "executedChangeSets";
    }

    public void addChangeset(ChangeSet changeSet) {
        changeSets.add(changeSet);

    }

    public List<ChangeSet> getChangeSets() {
        return changeSets;
    }

    public void setChangeSets(List<ChangeSet> changeSets) {
        this.changeSets = changeSets;
    }
}
