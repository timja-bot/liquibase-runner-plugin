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
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
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

        final int numberOfChangesetsToRollback = 2;
        FreeStyleBuild build =
                createAndLaunchRollbackJob(sunnyDayChangeset, configureRollbackCount(numberOfChangesetsToRollback));
        RolledbackChangesetAction action = build.getAction(RolledbackChangesetAction.class);

        assertThat(action.getBuild().getResult(), is(Result.SUCCESS));
        assertThat(action.getRolledbackChangesets(), hasSize(numberOfChangesetsToRollback));
    }

    @Test
    public void should_rollback_using_tag_sucessfully()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {

        FreeStyleBuild build = createAndLaunchRollbackJob(sunnyDayChangeset, configureToRollbackToFirstTag());

        assertThat(build.getResult(), is(Result.SUCCESS));

        RolledbackChangesetAction action = build.getAction(RolledbackChangesetAction.class);
        int amountOfChangesetsExistingAfterTag = 2;
        assertThat(action.getRolledbackChangesets(), hasSize(amountOfChangesetsExistingAfterTag));
    }

    @Test
    public void should_rollback_according_to_date()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {
        FreeStyleBuild build = createAndLaunchRollbackJob(sunnyDayChangeset, configureToRollbackAllChangesetsAppliedAfterInception());
        RolledbackChangesetAction action = build.getAction(RolledbackChangesetAction.class);

        int totalNumberOfChangesets = 4;

        assertThat(action.getRolledbackChangesets(), hasSize(totalNumberOfChangesets));
    }

    @Test
    public void should_indicate_rollback_error_as_unstable() throws IOException, SQLException,
            LiquibaseException, ExecutionException, InterruptedException {

        File changesetContainingError = LiquibaseTestUtil
                .createProjectFile(temporaryFolder, CHANGELOG_WITH_ROLLBACK_ERROR_RESOURCE_PATH);

        FreeStyleBuild build =
                createAndLaunchRollbackJob(changesetContainingError, configureRollbackCount(2));

        RolledbackChangesetAction action = build.getAction(RolledbackChangesetAction.class);

        assertThat(action, notNullValue());
        assertThat(build.getResult(), is(Result.UNSTABLE));
    }

    protected FreeStyleBuild createAndLaunchRollbackJob(File changesetFile,
                                                        RollStepConfigurer rollStepConfigurer)
            throws InterruptedException, ExecutionException, LiquibaseException, SQLException, IOException {
        createDatabaseUsingLiquibase(changesetFile);
        RollbackBuildStep rollbackBuildStep = createAndConfigureBuildStep(changesetFile, rollStepConfigurer);
        return addStepAndLaunchBuild(rollbackBuildStep);
    }

    protected static Date composeYesterdaysDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        return calendar.getTime();
    }


    protected FreeStyleBuild addStepAndLaunchBuild(RollbackBuildStep rollbackBuildStep)
            throws InterruptedException, ExecutionException {
        project.getBuildersList().add(rollbackBuildStep);
        return project.scheduleBuild2(0).get();
    }

    protected RollbackBuildStep createAndConfigureBuildStep(File changelogFile, RollStepConfigurer rollStepConfigurer) {
        RollbackBuildStep rollbackBuildStep = new RollbackBuildStep();
        rollbackBuildStep.setChangeLogFile(changelogFile.getAbsolutePath());
        rollbackBuildStep.setUrl(dbUrl);
        rollbackBuildStep.setDatabaseEngine("H2");
        rollStepConfigurer.configure(rollbackBuildStep);

        return rollbackBuildStep;
    }

    protected void createDatabaseUsingLiquibase(File changeset) throws IOException, SQLException, LiquibaseException {
        File inmemoryDatabaseFile = temporaryFolder.newFile();
        InputStream asStream = getClass().getResourceAsStream("/example-changesets/unit-test.h2.liquibase.properties");
        Properties liquibaseProperties = new Properties();
        liquibaseProperties.load(asStream);

        dbUrl = "jdbc:h2:file:" + inmemoryDatabaseFile.getAbsolutePath();
        Connection connection = DriverManager.getConnection(dbUrl, liquibaseProperties);
        JdbcConnection jdbcConnection = new JdbcConnection(connection);
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

        ClassLoaderResourceAccessor classLoaderResourceAccessor = new ClassLoaderResourceAccessor(ClassLoader.getSystemClassLoader());
        CompositeResourceAccessor resourceAccessor = new CompositeResourceAccessor(new FileSystemResourceAccessor(),
                classLoaderResourceAccessor);
        Liquibase liquibase = new Liquibase(changeset.getAbsolutePath(), resourceAccessor, database);
        liquibase.update(new Contexts());
    }

    static RollbackToFirstTag configureToRollbackToFirstTag() {
        return new RollbackToFirstTag();
    }

    protected static RollbackCount configureRollbackCount(int numberOfChangesetsToRollback) {
        return new RollbackCount(numberOfChangesetsToRollback);
    }

    static RollbackToYesterday configureToRollbackAllChangesetsAppliedAfterInception() {
        return new RollbackToYesterday();
    }


    private static class RollbackToYesterday implements RollStepConfigurer {

        @Override
        public void configure(RollbackBuildStep rollbackBuildStep) {
            rollbackBuildStep.setRollbackType(RollbackBuildStep.RollbackStrategy.DATE.name());
            Date yesterday = composeYesterdaysDate();
            rollbackBuildStep
                    .setRollbackToDate(new SimpleDateFormat(RollbackBuildStep.DATE_PATTERN).format(yesterday));
        }
    }

    private static class RollbackToFirstTag implements RollStepConfigurer {
        @Override
        public void configure(RollbackBuildStep rollbackBuildStep) {
            rollbackBuildStep.setRollbackType(RollbackBuildStep.RollbackStrategy.TAG.name());
            rollbackBuildStep.setRollbackToTag(FIRST_TAG);
        }
    }

    private static class RollbackCount implements RollStepConfigurer {
        private final int numberOfChangesetsToRollback;

        public RollbackCount(int numberOfChangesetsToRollback) {
            this.numberOfChangesetsToRollback = numberOfChangesetsToRollback;
        }

        @Override
        public void configure(RollbackBuildStep rollbackBuildStep) {
            rollbackBuildStep.setRollbackType(RollbackBuildStep.RollbackStrategy.COUNT.name());
            rollbackBuildStep.setNumberOfChangesetsToRollback(numberOfChangesetsToRollback);

        }
    }

    interface RollStepConfigurer {
        void configure(RollbackBuildStep rollbackBuildStep);
    }
}
