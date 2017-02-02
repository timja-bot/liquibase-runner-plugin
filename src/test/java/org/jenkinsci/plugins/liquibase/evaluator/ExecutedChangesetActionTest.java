package org.jenkinsci.plugins.liquibase.evaluator;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.jenkinsci.plugins.liquibase.matchers.IsChangeSetDetail.hasId;
import static org.junit.Assert.assertThat;

public class ExecutedChangesetActionTest {

    protected ExecutedChangesetAction executedChangesetAction = new ExecutedChangesetAction();

    @Before
    public void setup() {

    }

    @Test
    public void should_report_errors_present() {
        ChangeSetDetail changeSetDetail = new ChangeSetDetail();
        changeSetDetail.setExceptionMessage("Exception");
        executedChangesetAction.addChangeSetDetail(changeSetDetail);

        assertThat(executedChangesetAction.areErrorsPresent(), is(true));

    }

    @Test
    public void should_supply_failed_changesets() {
        String id = RandomStringUtils.randomAlphabetic(5);
        ChangeSetDetail changeSetDetail =
                new ChangeSetDetail.Builder().withId(id).withSuccessfullyExecuted(false).build();
        executedChangesetAction.addChangeSetDetail(changeSetDetail);

        List<ChangeSetDetail> failedChangeSets = executedChangesetAction.getFailedChangeSets();
        assertThat(failedChangeSets, hasSize(1));
        assertThat(failedChangeSets, contains(hasId(id)));
    }

    @Test
    public void should_supply_successful_changesets() {
        String id = RandomStringUtils.randomAlphabetic(5);
        ChangeSetDetail changeSetDetail = new ChangeSetDetail.Builder().withId(id).withSuccessfullyExecuted(true).build();
        executedChangesetAction.addChangeSetDetail(changeSetDetail);
        List<ChangeSetDetail> successfulChangeSets = executedChangesetAction.getSuccessfulChangeSets();
        assertThat(successfulChangeSets, hasSize(1));
        assertThat(successfulChangeSets, contains(hasId(id)));

    }


}