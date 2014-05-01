package org.jenkinsci.plugins.liquibase.builder;

import liquibase.change.Change;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.precondition.core.PreconditionContainer;
import liquibase.statement.SqlStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildChangeExecListener implements ChangeExecListener {
    private final ExecutedChangesetAction action;
    private static final Logger LOG = LoggerFactory.getLogger(BuildChangeExecListener.class);

    public BuildChangeExecListener(ExecutedChangesetAction action) {
        this.action = action;
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

        if (LOG.isDebugEnabled()) {
            LOG.debug("adding changeset to action.  id:" + changeSet.getId());
        }
        action.addChangeset(changeSet);
    }

    public void rolledBack(ChangeSet changeSet, DatabaseChangeLog databaseChangeLog, Database database) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("rolling back changeset[" + changeSet.getId() + "] ");
        }


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
        SqlStatement[] sqlStatements = change.generateStatements(database);

    }
}
