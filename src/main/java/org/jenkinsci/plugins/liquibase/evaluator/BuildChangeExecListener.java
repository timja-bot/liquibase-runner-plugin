package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.model.TaskListener;
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
 * Listens for changeset execution to add them to {@link org.jenkinsci.plugins.liquibase.evaluator.ExecutedChangesetAction}.
 */
public class BuildChangeExecListener implements ChangeExecListener {
    private final ExecutedChangesetAction action;
    private static final Logger LOG = LoggerFactory.getLogger(BuildChangeExecListener.class);
    private TaskListener taskListener;
    private static final String RAN_CHANGESET_MSG = "Ran changeset: ";

    public BuildChangeExecListener(ExecutedChangesetAction action) {
        this.action = action;
    }

    public BuildChangeExecListener(ExecutedChangesetAction action, TaskListener taskListener) {
        this.action = action;
        this.taskListener = taskListener;
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

        String logMessage = formatChangesetForLog(changeSet, "Rolled back");
        taskListener.getLogger().println(logMessage);
        ChangeSetDetail changeSetDetail = ChangeSetDetail.fromChangeSet(changeSet);
        action.addRolledBackChangesetDetail(changeSetDetail);
        if (action.hasChangesetWithId(changeSetDetail.getId())) {
            action.markChangesetAsRolledBack(changeSetDetail.getId());
        }
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
        printConsoleLogMessage(changeSet);

        ChangeSetDetail changeSetDetail = createChangeSetDetail(change, changeSet, database);
        action.addChangeSetDetail(changeSetDetail);

    }

    protected static ChangeSetDetail createChangeSetDetail(Change change, ChangeSet changeSet, Database database) {
        SqlStatement[] sqlStatements = change.generateStatements(database);
        List<Sql> statementSqls = Lists.newArrayList();
        if (sqlStatements != null && sqlStatements.length > 0) {
            for (SqlStatement sqlStatement : sqlStatements) {
                Sql[] sqls = SqlGeneratorFactory.getInstance().generateSql(sqlStatement, database);
                statementSqls.addAll(Arrays.asList(sqls));
            }
        }
        ChangeSetDetail changeSetDetail = ChangeSetDetail.fromChangeSet(changeSet, statementSqls);
        return changeSetDetail;
    }

    protected void printConsoleLogMessage(ChangeSet changeSet) {
        String logMessage = Util.formatChangeset(changeSet);
        taskListener.getLogger().println(RAN_CHANGESET_MSG + logMessage);
    }

    public void runFailed(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database, Exception e) {
        ChangeSetDetail changeSetDetail = ChangeSetDetail.createFailed(changeSet, e);
        action.addChangeSetDetail(changeSetDetail);
    }

    public static String formatChangesetForLog(ChangeSet changeSet, String msg) {
        String changeSetLogMsg = Util.formatChangeset(changeSet);
        return changeSetLogMsg + ": " + msg;
    }

}
