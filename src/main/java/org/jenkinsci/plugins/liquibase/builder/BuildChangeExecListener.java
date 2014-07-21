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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        action.addChangeset(changeSet);

    }

    public void ran(Change change, ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("in 'ran' of listener, attempting to get sql.");
        }
        SqlStatement[] sqlStatements = change.generateStatements(database);

        String msg = "Ran changeset: ";
        String logMessage = formatChangeset(changeSet, changeLog);
        buildListener.getLogger().println(msg + logMessage);

        if (sqlStatements != null && sqlStatements.length > 0) {
            SqlStatement sqlStatement = sqlStatements[0];
            Sql[] sqls = SqlGeneratorFactory.getInstance().generateSql(sqlStatement, database);
            action.addChangesetWithSql(changeSet, sqls);
            for (Sql sql : sqls) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("sql.toSql():[" + sql.toSql() + "]");
                }
            }
        }

    }

    private static String formatChangesetForLog(ChangeSet changeSet, DatabaseChangeLog changeLog, String msg) {
        String changeSetLogMsg = formatChangeset(changeSet, changeLog);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(changeSetLogMsg).append(": ").append(msg);
        return stringBuilder.toString();
    }

    private static String formatChangeset(ChangeSet changeSet, DatabaseChangeLog changeLog) {
        String filePath;
        if (changeLog != null) {
            filePath = changeLog.getFilePath();
        } else {
            filePath = "";
        }
        String changeSetName;
        if (changeSet != null) {
            changeSetName = changeSet.toString(false);
        } else {
            changeSetName = "";
        }
        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append(filePath).append(": ");
        msgBuilder.append(changeSetName.replace(filePath + "::", ""));

        return msgBuilder.toString();
    }

}
