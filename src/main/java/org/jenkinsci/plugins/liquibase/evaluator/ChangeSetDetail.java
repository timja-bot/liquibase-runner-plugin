package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.model.Action;
import liquibase.changelog.ChangeSet;
import liquibase.sql.Sql;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

/**
 * A shallow copy of @{link Changest} for use to display in jenkins.
 */
public class ChangeSetDetail implements Action {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeSetDetail.class);

    public static final int MAX_LINES = 15;
    private List<Sql> sqls;
    private boolean successfullyExecuted = true;
    private ExecutedChangesetAction parent;
    private String author;
    private String id;
    private String comments;
    private String description;
    private String filePath;
    private String exceptionMessage;
    private boolean rolledBack;


    public ChangeSetDetail() {

    }

    private ChangeSetDetail(Builder builder) {
        setSuccessfullyExecuted(builder.successfullyExecuted);
        setAuthor(builder.author);
        setId(builder.id);
        setComments(builder.comments);
        setDescription(builder.description);
    }

    public static ChangeSetDetail fromChangeSet(ChangeSet changeSet) {
        ChangeSetDetail changeSetDetail = new ChangeSetDetail();
        changeSetDetail.setFilePath(changeSet.getFilePath());
        changeSetDetail.setId(changeSet.getId());
        changeSetDetail.setAuthor(changeSet.getAuthor());
        changeSetDetail.setComments(changeSet.getComments());
        changeSetDetail.setDescription(changeSet.getDescription());
        return changeSetDetail;
    }

    public static ChangeSetDetail fromChangeSet(ChangeSet changeSet, List<Sql> sqlList) {
        ChangeSetDetail changeSetDetail = ChangeSetDetail.fromChangeSet(changeSet);
        changeSetDetail.setSqls(sqlList);
        return changeSetDetail;
    }

    public static ChangeSetDetail createFailed(ChangeSet changeSet, Exception e) {
        ChangeSetDetail failedChangeset = ChangeSetDetail.fromChangeSet(changeSet);
        failedChangeset.setSuccessfullyExecuted(false);
        failedChangeset.setExceptionMessage(e.getMessage());
        return failedChangeset;
    }


    public String getExecutedSql() {
        StringBuilder sb = new StringBuilder();
        if (sqls != null) {
            for (Sql changesetSql : sqls) {
                sb.append(changesetSql.toSql()).append('\n');
            }
        }

        return sb.toString();
    }

    public boolean isInNeedOfTruncate() {
        return StringUtils.countMatches(getExecutedSql(), "\n") > MAX_LINES;
    }

    public boolean hasSql() {
        return sqls != null && !getExecutedSql().isEmpty();
    }

    public String getTruncatedSql() {
        String executedSql = getExecutedSql();
        return truncateString(executedSql);
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

        if (!parent.equals(that.parent)) {
            return false;
        }
        if (author != null ? !author.equals(that.author) : that.author != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (filePath != null ? !filePath.equals(that.filePath) : that.filePath != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = parent.hashCode();
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
        return result;
    }

    protected static String truncateString(String executedSql) {
        Iterable<String> strings = Splitter.on('\n').split(executedSql);
        Iterable<String> truncated = Iterables.limit(strings, MAX_LINES);
        String join = Joiner.on('\n').join(truncated);
        return join;
    }

    public List<Sql> getSqls() {
        return sqls;
    }

    public boolean isSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    public void setSuccessfullyExecuted(boolean successfullyExecuted) {
        this.successfullyExecuted = successfullyExecuted;
    }

    public void addSql(Sql sql) {
        if (!sqls.contains(sql)) {
            sqls.add(sql);
        }
    }


    @Override
    public String toString() {
        return "ChangeSetDetail{" +
                "sqls=" + sqls +
                ", successfullyExecuted=" + successfullyExecuted +
                ", author='" + author + '\'' +
                ", id='" + id + '\'' +
                ", comments='" + comments + '\'' +
                ", description='" + description + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }


    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return id;
    }

    @Override
    public String getUrlName() {
        return id;
    }

    public ExecutedChangesetAction getParent() {
        return parent;
    }

    public void setParent(ExecutedChangesetAction parent) {
        this.parent = parent;
    }

    public boolean hasExceptionMessage() {
        return !Strings.isNullOrEmpty(exceptionMessage);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public void setSqls(List<Sql> sqls) {
        this.sqls = sqls;
    }


    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public void setRolledBack(boolean rolledBack) {
        this.rolledBack = rolledBack;
    }

    public static final class Builder {
        private boolean successfullyExecuted;
        private String author;
        private String id;
        private String comments;
        private String description;


        public Builder() {
        }

        public Builder withSuccessfullyExecuted(boolean val) {
            successfullyExecuted = val;
            return this;
        }

        public Builder withAuthor(String val) {
            author = val;
            return this;
        }

        public Builder withId(String val) {
            id = val;
            return this;
        }

        public Builder withComments(String val) {
            comments = val;
            return this;
        }

        public Builder withDescription(String val) {
            description = val;
            return this;
        }

        public ChangeSetDetail build() {
            return new ChangeSetDetail(this);
        }


    }
}
