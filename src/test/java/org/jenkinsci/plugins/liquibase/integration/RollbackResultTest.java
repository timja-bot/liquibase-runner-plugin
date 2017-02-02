package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import liquibase.exception.LiquibaseException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.jenkinsci.plugins.liquibase.evaluator.RollbackBuilder;
import org.jenkinsci.plugins.liquibase.evaluator.RolledbackChangesetAction;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.jenkinsci.plugins.liquibase.matchers.BuildResultMatcher.isSuccessful;
import static org.jenkinsci.plugins.liquibase.matchers.BuildResultMatcher.isUnstable;
import static org.jenkinsci.plugins.liquibase.matchers.IsChangeSetDetail.hasId;
import static org.junit.Assert.assertThat;

public class RollbackResultTest {

    private static final Logger LOG = LoggerFactory.getLogger(RollbackResultTest.class);
    private static final String FIRST_TAG = "first_tag";
    private static final String CHANGELOG_WITH_ROLLBACK_ERROR_RESOURCE_PATH =
            "/example-changesets/changeset-with-rollback-error.xml";

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected FreeStyleProject project;
    protected File sunnyDayChangeset;
    protected String jdbcUrl;

    @Before
    public void setup() throws SQLException, IOException, LiquibaseException {
        temporaryFolder.create();
        project = jenkinsRule.createFreeStyleProject();
        sunnyDayChangeset = LiquibaseTestUtil.createErrorFreeChangeset(temporaryFolder);
        jdbcUrl = LiquibaseTestUtil.composeJdbcUrl(temporaryFolder.newFile());

    }

    @Test
    public void should_report_success_with_successful_rollbacks()
            throws IOException, ExecutionException, InterruptedException, SQLException, LiquibaseException {

        LiquibaseTestUtil.createDatabase(jdbcUrl, sunnyDayChangeset);

        RollbackBuilder buildStep = createBaseBuildStep(RollbackBuilder.RollbackStrategy.COUNT, sunnyDayChangeset,
                jdbcUrl);
        int numberOfChangesetsToRollback = 2;
        buildStep.setNumberOfChangesetsToRollback(String.valueOf(numberOfChangesetsToRollback));
        RolledbackChangesetAction action = launchBuild(buildStep);

        assertThat(action.getBuild(), isSuccessful());
        assertThat(action.getRolledbackChangesets(), hasSize(numberOfChangesetsToRollback));
    }

    @Test
    public void should_rollback_using_tag_sucessfully()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {

        LiquibaseTestUtil.createDatabase(jdbcUrl, sunnyDayChangeset);
        RollbackBuilder rollbackBuildStep =
                createBaseBuildStep(RollbackBuilder.RollbackStrategy.TAG, sunnyDayChangeset,
                        jdbcUrl);
        rollbackBuildStep.setRollbackToTag(FIRST_TAG);

        RolledbackChangesetAction action = launchBuild(rollbackBuildStep);


        assertThat(action.getBuild(), isSuccessful());
        int expectedChangesetsToBeRolledBack = 3;
        assertThat(action.getRolledbackChangesets(), hasSize(expectedChangesetsToBeRolledBack));
    }

    @Test
    public void should_rollback_according_to_date()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {
        LiquibaseTestUtil.createDatabase(jdbcUrl, sunnyDayChangeset);
        RollbackBuilder rollbackBuildStep =
                createBaseBuildStep(RollbackBuilder.RollbackStrategy.DATE, sunnyDayChangeset, jdbcUrl);
        Date yesterday = dateBeforeChangesetsApplied();
        rollbackBuildStep.setRollbackToDate(new SimpleDateFormat(RollbackBuilder.DATE_PATTERN).format(yesterday));

        RolledbackChangesetAction resultAction = launchBuild(rollbackBuildStep);

        assertThat(resultAction.getRolledbackChangesets(), hasItems(
                hasId("create-table"),
                hasId("first_tag"),
                hasId("create-color-table"),
                hasId("create-testing-table"))
        );
        int totalNumberOfChangesets = 4;
        assertThat(resultAction.getRolledbackChangesets(), hasSize(totalNumberOfChangesets));
    }

    @Test
    public void should_indicate_rollback_error_as_unstable() throws IOException, SQLException,
            LiquibaseException, ExecutionException, InterruptedException {

        File changesetContainingError = LiquibaseTestUtil
                .createFileFromResource(temporaryFolder.getRoot(), CHANGELOG_WITH_ROLLBACK_ERROR_RESOURCE_PATH);

        LiquibaseTestUtil.createDatabase(jdbcUrl, changesetContainingError);

        RollbackBuilder buildStep =
                createBaseBuildStep(RollbackBuilder.RollbackStrategy.COUNT, changesetContainingError, jdbcUrl);
        buildStep.setNumberOfChangesetsToRollback(String.valueOf(2));

        RolledbackChangesetAction resultAction = launchBuild(buildStep);
        assertThat(resultAction, notNullValue());
        assertThat(resultAction.getBuild(), isUnstable());

    }

    protected static RollbackBuilder createBaseBuildStep(RollbackBuilder.RollbackStrategy rollbackStrategy,
                                                         File changelogFile, String dbUrl) {
        RollbackBuilder rollbackBuildStep = new RollbackBuilder();
        rollbackBuildStep.setChangeLogFile(changelogFile.getAbsolutePath());
        rollbackBuildStep.setUrl(dbUrl);
        rollbackBuildStep.setDatabaseEngine("H2");
        rollbackBuildStep.setRollbackType(rollbackStrategy.name());
        return rollbackBuildStep;
    }

    protected RolledbackChangesetAction launchBuild(RollbackBuilder buildStep)
            throws InterruptedException, ExecutionException {
        project.getBuildersList().add(buildStep);
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        return build.getAction(RolledbackChangesetAction.class);
    }

    protected static Date dateBeforeChangesetsApplied() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        return calendar.getTime();
    }
}
