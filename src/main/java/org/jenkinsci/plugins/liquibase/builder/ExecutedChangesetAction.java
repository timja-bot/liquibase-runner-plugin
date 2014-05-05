package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.Action;
import liquibase.changelog.ChangeSet;
import liquibase.sql.Sql;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;

public class ExecutedChangesetAction implements Action {

    ArrayListMultimap<ChangeSet, Sql> sqlsMap = ArrayListMultimap.create();

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

    public List<Sql> getSql(ChangeSet changeSet) {

        return sqlsMap.get(changeSet);
    }

    public void addSql(ChangeSet changeSet, Sql[] sqls) {
        sqlsMap.putAll(changeSet, Arrays.asList(sqls));

    }

    public void addChangesetWithSql(ChangeSet changeSet, Sql[] sqls) {
        changeSets.add(changeSet);
        addSql(changeSet, sqls);

    }
    public void setChangeSets(List<ChangeSet> changeSets) {
        this.changeSets = changeSets;
    }
}
