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
 *
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
            LOG.debug("rolling back changeset[" + changeSet.getId() + "] ");
        }
        buildMessage("Successfully rolled back", changeSet, databaseChangeLog);
    }

    public void preconditionFailed(PreconditionFailedException error,
                                   PreconditionContainer.FailOption onFail) {

    }

    public void preconditionErrored(PreconditionErrorException error,
                                    PreconditionContainer.ErrorOption onError) {

    }

    public void willRun(Change change,
                        ChangeSet changeSet,
                        DatabaseChangeLog changeLog,
                        Database database) {


    }

    public void ran(Change change, ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("in 'ran' of listener, attempting to get sql.");
        }
        SqlStatement[] sqlStatements = change.generateStatements(database);

        String msg = "Ran successfully";
        buildMessage(msg, changeSet, changeLog);

        if (sqlStatements != null && sqlStatements.length > 0) {
            SqlStatement sqlStatement = sqlStatements[0];
            Sql[] sqls = SqlGeneratorFactory.getInstance().generateSql(sqlStatement, database);
            action.addChangesetWithSql(changeSet, sqls);

            for (int i = 0; i < sqls.length; i++) {
                Sql sql = sqls[i];
                if(LOG.isDebugEnabled()) {
                	LOG.debug("sql.toSql():[" + sql.toSql() + "]");
                }
            }
        }

    }

    private void buildMessage(String msg, ChangeSet changeSet, DatabaseChangeLog changeLog) {
        String filePath = null;
        if (changeLog!=null) {
            filePath = changeLog.getFilePath();
        }                  else {
            filePath="";

        }
        String changeSetName;
        if (changeSet!=null) {
            changeSetName = changeSet.toString(false);
        } else {
            changeSetName = "";
        }
        String logMessage = buildMessage(msg, filePath, changeSetName);
        buildListener.getLogger().println(logMessage);
    }

    private String buildMessage(String message, String changeLogName, String changeSetName) {
        StringBuilder msg = new StringBuilder();
        if (changeLogName != null) {
            msg.append(changeLogName).append(": ");
        }
        if (changeSetName != null) {
            msg.append(changeSetName.replace(changeLogName + "::", "")).append(": ");
        }
        msg.append(message);
        return msg.toString();
    }
}
