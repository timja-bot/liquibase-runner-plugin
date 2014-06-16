package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import liquibase.changelog.ChangeSet;
import liquibase.sql.Sql;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;

/**
 * Supplies information any executed changesets to a particular build.
 */
public class ExecutedChangesetAction implements Action {

    private AbstractBuild<?,?> build;

    ArrayListMultimap<ChangeSet, Sql> sqlsMap = ArrayListMultimap.create();

    Set<ChangeSet> changeSets = Sets.newHashSet();

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
        changeSets.add(changeSet);
    }

    public Set<ChangeSet> getChangeSets() {
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
        if (!sqlsMap.containsKey(changeSet)) {
            addSql(changeSet, sqls);
        }
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public void setBuild(AbstractBuild<?, ?> build) {
        this.build = build;
    }
}
