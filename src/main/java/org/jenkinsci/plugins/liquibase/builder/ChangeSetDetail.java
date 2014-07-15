package org.jenkinsci.plugins.liquibase.builder;

import liquibase.changelog.ChangeSet;
import liquibase.sql.Sql;

import java.util.Arrays;
import java.util.List;

public class ChangeSetDetail {

    private List<Sql> sqls;
    private Sql sql;
    private ChangeSet changeSet;

    private ChangeSetDetail(ChangeSet changeSet, List<Sql> sqls) {
        this.sqls = sqls;
        this.changeSet = changeSet;
    }

    private ChangeSetDetail(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    public Sql getSql() {
        return sql;
    }

    public List<Sql> getSqls() {
        return sqls;
    }

    public ChangeSet getChangeSet() {
        return changeSet;
    }

    public static ChangeSetDetail createWithSql(ChangeSet changeSet, Sql[] sqls) {
        return new ChangeSetDetail(changeSet, Arrays.asList(sqls));
    }

    public static ChangeSetDetail create(ChangeSet changeSet) {
        return new ChangeSetDetail(changeSet);
    }
}
