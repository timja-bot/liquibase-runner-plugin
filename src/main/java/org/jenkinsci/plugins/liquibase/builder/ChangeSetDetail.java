package org.jenkinsci.plugins.liquibase.builder;

import liquibase.changelog.ChangeSet;
import liquibase.sql.Sql;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.liquibase.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class ChangeSetDetail {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeSetDetail.class);

    public static final int MAX_LINES = 15;
    private List<Sql> sqls;
    private Sql sql;
    private ChangeSet changeSet;

    public ChangeSetDetail() {

    }

    private ChangeSetDetail(ChangeSet changeSet, List<Sql> sqls) {
        this.sqls = sqls;
        this.changeSet = changeSet;
    }

    private ChangeSetDetail(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    public String getExecutedSql() {
        StringBuilder sb = new StringBuilder();
        for (Sql changesetSql : sqls) {
            sb.append(changesetSql.toSql()).append('\n');
        }

        return sb.toString();
    }

    public boolean isInNeedOfTruncate() {
        return StringUtils.countMatches(getExecutedSql(), "\n") > MAX_LINES;
    }

    public String getTruncatedSql() {
        String executedSql = getExecutedSql();
        return truncateString(executedSql);
    }

    protected static String truncateString(String executedSql) {
        Iterable<String> strings = Splitter.on('\n').split(executedSql);
        Iterable<String> truncated = Iterables.limit(strings, MAX_LINES);
        String join = Joiner.on('\n').join(truncated);
        return join;
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

    public void addSql(Sql sql) {
        if (!sqls.contains(sql)) {
            sqls.add(sql);
        }

    }
    public static ChangeSetDetail createWithSql(ChangeSet changeSet, Sql[] sqls) {
        List<Sql> sqlList = Arrays.asList(sqls);
        return createWithSql(changeSet, sqlList);
    }

    public static ChangeSetDetail createWithSql(ChangeSet changeSet, List<Sql> sqlList) {
        return new ChangeSetDetail(changeSet, sqlList);
    }

    public static ChangeSetDetail create(ChangeSet changeSet) {
        return new ChangeSetDetail(changeSet);
    }

    @Override
    public String toString() {
        return "ChangeSetDetail{" +
                "changeSet=" + Util.formatChangeset(changeSet) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChangeSetDetail)) {
            return false;
        }

        ChangeSetDetail that = (ChangeSetDetail) o;

        if (!changeSet.equals(that.changeSet)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return changeSet.hashCode();
    }
}
