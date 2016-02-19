package org.jenkinsci.plugins.liquibase.builder;

import hudson.model.BuildListener;
import liquibase.change.core.AddColumnChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.core.H2Database;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnit44Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnit44Runner.class)
public class BuildChangeExecListenerTest {

    private static final Logger LOG = LoggerFactory.getLogger(BuildChangeExecListenerTest.class);


    @Mock
    BuildListener buildListener;

    @Test

    public void shouldContainExpectedChangesetAction() {
        ExecutedChangesetAction changesetAction = new ExecutedChangesetAction();
        BuildChangeExecListener listener = new BuildChangeExecListener(changesetAction, buildListener);

        when(buildListener.getLogger()).thenReturn(System.out);

        DatabaseChangeLog databaseChangeLog = new DatabaseChangeLog();
        listener.ran(new AddColumnChange(), new ChangeSet(databaseChangeLog), databaseChangeLog, new H2Database());
        List<ChangeSetAction> changeSetActions = changesetAction.getChangeSetActions();

        assertThat(changesetAction.getChangeSetDetails(), hasSize(1));


    }

}