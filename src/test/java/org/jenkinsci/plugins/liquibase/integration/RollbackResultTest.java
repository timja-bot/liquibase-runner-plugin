package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.jenkinsci.plugins.liquibase.evaluator.RollbackBuildStep;
import org.jenkinsci.plugins.liquibase.evaluator.RolledbackChangesetAction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class RollbackResultTest {

    private static final Logger LOG = LoggerFactory.getLogger(RollbackResultTest.class);
    private static final String FIRST_TAG = "first_tag";
    private static final String CHANGELOG_WITH_ROLLBACK_ERROR_RESOURCE_PATH =
            "/example-changesets/changeset-with-rollback-error.xml";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected String dbUrl;
    protected FreeStyleProject project;
    protected File sunnyDayChangeset;

    @Before
    public void setup() throws SQLException, IOException, LiquibaseException {
        temporaryFolder.create();
        project = jenkinsRule.createFreeStyleProject();
        sunnyDayChangeset = LiquibaseTestUtil.createErrorFreeChangeset(temporaryFolder);
    }

    @Test
    public void should_report_success_with_successful_rollbacks()
            throws IOException, ExecutionException, InterruptedException, SQLException, LiquibaseException {

        createDatabase(sunnyDayChangeset);

        RollbackBuildStep buildStep = createBaseBuildStep(RollbackBuildStep.RollbackStrategy.COUNT, sunnyDayChangeset,
                dbUrl);
        int numberOfChangesetsToRollback = 2;
        buildStep.setNumberOfChangesetsToRollback(numberOfChangesetsToRollback);
        RolledbackChangesetAction action = launchBuild(buildStep);

        assertThat(action.getBuild().getResult(), is(Result.SUCCESS));
        assertThat(action.getRolledbackChangesets(), hasSize(numberOfChangesetsToRollback));
    }

    @Test
    public void should_rollback_using_tag_sucessfully()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {

        createDatabase(sunnyDayChangeset);
        RollbackBuildStep rollbackBuildStep = createBaseBuildStep(RollbackBuildStep.RollbackStrategy.TAG, sunnyDayChangeset,
                dbUrl);
        rollbackBuildStep.setRollbackToTag(FIRST_TAG);

        RolledbackChangesetAction action = launchBuild(rollbackBuildStep);

        assertThat(action.getBuild().getResult(), is(Result.SUCCESS));
        int amountOfChangesetsExistingAfterTag = 2;
        assertThat(action.getRolledbackChangesets(), hasSize(amountOfChangesetsExistingAfterTag));
    }

    @Test
    public void should_rollback_according_to_date()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {
        createDatabase(sunnyDayChangeset);
        RollbackBuildStep rollbackBuildStep =
                createBaseBuildStep(RollbackBuildStep.RollbackStrategy.DATE, sunnyDayChangeset, dbUrl);
        Date yesterday = dateBeforeChangesetsApplied();
        rollbackBuildStep.setRollbackToDate(new SimpleDateFormat(RollbackBuildStep.DATE_PATTERN).format(yesterday));

        RolledbackChangesetAction resultAction = launchBuild(rollbackBuildStep);

        int totalNumberOfChangesets = 4;

        assertThat(resultAction.getRolledbackChangesets(), hasSize(totalNumberOfChangesets));
    }

    @Test
    public void should_indicate_rollback_error_as_unstable() throws IOException, SQLException,
            LiquibaseException, ExecutionException, InterruptedException {

        File changesetContainingError = LiquibaseTestUtil
                .createFileFromResource(temporaryFolder.getRoot(), CHANGELOG_WITH_ROLLBACK_ERROR_RESOURCE_PATH);

        createDatabase(changesetContainingError);

        RollbackBuildStep buildStep =
                createBaseBuildStep(RollbackBuildStep.RollbackStrategy.COUNT, changesetContainingError, dbUrl);
        buildStep.setNumberOfChangesetsToRollback(2);

        RolledbackChangesetAction resultAction = launchBuild(buildStep);
        assertThat(resultAction, notNullValue());
        assertThat(resultAction.getBuild().getResult(), is(Result.UNSTABLE));
    }

    protected void createDatabase(File changeset) throws IOException, SQLException, LiquibaseException {
        File inmemoryDatabaseFile = temporaryFolder.newFile();
        InputStream asStream = getClass().getResourceAsStream("/example-changesets/unit-test.h2.liquibase.properties");
        Properties liquibaseProperties = new Properties();
        liquibaseProperties.load(asStream);

        dbUrl = "jdbc:h2:file:" + inmemoryDatabaseFile.getAbsolutePath();
        Connection connection = DriverManager.getConnection(dbUrl, liquibaseProperties);
        JdbcConnection jdbcConnection = new JdbcConnection(connection);
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

        Liquibase liquibase = new Liquibase(changeset.getAbsolutePath(), new FileSystemResourceAccessor(), database);
        liquibase.update(new Contexts());
    }

    protected static RollbackBuildStep createBaseBuildStep(RollbackBuildStep.RollbackStrategy rollbackStrategy,
                                                           File changelogFile, String dbUrl) {
        RollbackBuildStep buildStep1 = new RollbackBuildStep();
        buildStep1.setChangeLogFile(changelogFile.getAbsolutePath());
        buildStep1.setUrl(dbUrl);
        buildStep1.setDatabaseEngine("H2");
        RollbackBuildStep buildStep = buildStep1;
        buildStep.setRollbackType(rollbackStrategy.name());
        return buildStep;
    }

    protected RolledbackChangesetAction launchBuild(RollbackBuildStep buildStep)
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
