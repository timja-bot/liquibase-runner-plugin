package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.BuildListener;
import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

import java.util.Arrays;
import java.util.List;

import org.jenkinsci.plugins.liquibase.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Listens for changeset execution to add them to {@link org.jenkinsci.plugins.liquibase.builder.ExecutedChangesetAction}.
 */
public class BuildChangeExecListener implements ChangeExecListener {
    private final ExecutedChangesetAction action;
    private static final Logger LOG = LoggerFactory.getLogger(BuildChangeExecListener.class);
    private BuildListener buildListener;

    public BuildChangeExecListener(ExecutedChangesetAction action) {
        this.action = action;
    }

    public BuildChangeExecListener(ExecutedChangesetAction action, BuildListener buildListener) {
        this.action = action;
        this.buildListener = buildListener;
    }

    public void willRun(ChangeSet changeSet,
                        DatabaseChangeLog databaseChangeLog,
                        Database database,
                        ChangeSet.RunStatus runStatus) {
    }

    public void ran(ChangeSet changeSet,
                    DatabaseChangeLog databaseChangeLog,
                    Database database,
                    ChangeSet.ExecType execType) {
    }

    public void rolledBack(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("rolling back changeset [" + changeSet.getId() + "] ");
        }
        String logMessage = formatChangesetForLog(changeSet, databaseChangeLog, "Rolled back");
        buildListener.getLogger().println(logMessage);
    }

    public void preconditionFailed(PreconditionFailedException error, PreconditionContainer.FailOption onFail) {
    }

    public void preconditionErrored(PreconditionErrorException error, PreconditionContainer.ErrorOption onError) {
    }

    public void willRun(Change change, ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        boolean debugEnabled = LOG.isDebugEnabled();
        if (debugEnabled) {
            LOG.debug("will run[" + change + "] ");
        }
    }

    public void ran(Change change, ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("in 'ran' of listener, attempting to get sql.");
        }
        SqlStatement[] sqlStatements = change.generateStatements(database);

        String msg = "Ran changeset: ";
        String logMessage = Util.formatChangeset(changeSet);
        buildListener.getLogger().println(msg + logMessage);

        List<Sql> statementSqls = Lists.newArrayList();
        if (sqlStatements != null && sqlStatements.length > 0) {
            for (SqlStatement sqlStatement : sqlStatements) {
                Sql[] sqls = SqlGeneratorFactory.getInstance().generateSql(sqlStatement, database);
                statementSqls.addAll(Arrays.asList(sqls));
            }
            action.addChangesetWithSql(changeSet, statementSqls);
        }
    }

    public void runFailed(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database, Exception e) {
        action.addFailed(changeSet);
    }

    public static String formatChangesetForLog(ChangeSet changeSet, DatabaseChangeLog changeLog, String msg) {
        String changeSetLogMsg = Util.formatChangeset(changeSet);
        return changeSetLogMsg + ": " + msg;
    }
}
