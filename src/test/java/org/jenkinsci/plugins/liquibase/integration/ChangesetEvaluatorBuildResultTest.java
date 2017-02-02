package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.ExecutedChangesetAction;
import org.jenkinsci.plugins.liquibase.matchers.IsChangeSetDetail;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.jenkinsci.plugins.liquibase.matchers.BuildResultMatcher.isFailure;
import static org.jenkinsci.plugins.liquibase.matchers.BuildResultMatcher.isSuccessful;
import static org.jenkinsci.plugins.liquibase.matchers.BuildResultMatcher.isUnstable;
import static org.jenkinsci.plugins.liquibase.matchers.IsChangeSetDetail.hasId;
import static org.jenkinsci.plugins.liquibase.matchers.IsChangeSetDetail.isChangeSetDetail;
import static org.junit.Assert.assertThat;

public class ChangesetEvaluatorBuildResultTest {

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluatorBuildResultTest.class);
    private static final String LIQUIBASE_PROPERTIES = "/example-changesets/h2.liquibase.properties";

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        temporaryFolder.create();
    }

    @Test
    public void should_report_success_with_error_free_changeset()
            throws IOException, ExecutionException, InterruptedException {

        FreeStyleBuild freeStyleBuild = createAndBuildErrorFreeProject();
        assertThat(freeStyleBuild, isSuccessful());
    }

    @Test
    public void should_indicate_unstable_build_when_changeset_has_error()
            throws IOException, ExecutionException, InterruptedException {
        File changesetFileWithError = LiquibaseTestUtil.createChangesetFileWithError(temporaryFolder);
        FreeStyleProject project = createProjectWithChangelogFile(changesetFileWithError);
        FreeStyleBuild build = launchBuildForProject(project);
        assertThat(build, isUnstable());
    }

    @Test
    public void should_indicate_success_with_yaml_formatted_changeset()
            throws IOException, ExecutionException, InterruptedException {
        FreeStyleBuild build = createAndBuildLiquibaseProject("/example-changesets/yaml-changeset-sunnyday.yml");
        assertThat(build, isSuccessful());
    }

    @Test
    public void should_mark_liquibase_setup_problem_as_failure()
            throws IOException, ExecutionException, InterruptedException {

        Properties properties = new Properties();
        properties.setProperty("driver", "nosuch.driver");
        properties.setProperty("changeLogFile", "sunny-day-changeset.xml");
        LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(), "/example-changesets/sunny-day-changeset.xml");

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.setCustomWorkspace(temporaryFolder.getRoot().getAbsolutePath());

        File propertiesFile = temporaryFolder.newFile();
        properties.store(new FileOutputStream(propertiesFile), "Liquibase Test Properties");

        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setLiquibasePropertiesPath(propertiesFile.getAbsolutePath());

        project.getBuildersList().add(evaluator);
        FreeStyleBuild build = launchBuildForProject(project);
        assertThat(build , isFailure());

    }



    /**
     * Covers https://github.com/jenkinsci/liquibase-runner-plugin/issues/8
     */
    @Test
    public void should_use_liquibase_defaults_file() throws InterruptedException, ExecutionException, IOException {

        LiquibaseTestUtil
                .createFilesFromResources(temporaryFolder, LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML,
                        LIQUIBASE_PROPERTIES);
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.setCustomWorkspace(temporaryFolder.getRoot().getAbsolutePath());
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setLiquibasePropertiesPath(LiquibaseTestUtil.extractFilenameFromResourcePath(LIQUIBASE_PROPERTIES));
        project.getBuildersList().add(evaluator);
        FreeStyleBuild build = launchBuildForProject(project);
        assertThat(build, isSuccessful());
    }


    @Test
    public void should_locate_change_log_using_basepath()
            throws IOException, ExecutionException, InterruptedException, SQLException, LiquibaseException {
        File changesetDir = temporaryFolder.newFolder("changesetDir");
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        File changeLog =
                LiquibaseTestUtil.createFileFromResource(changesetDir, "/example-changesets/single-changeset.xml");
        evaluator.setChangeLogFile(
                changeLog.getAbsolutePath());
        evaluator.setUrl(LiquibaseTestUtil.IN_MEMORY_JDBC_URL);
        evaluator.setDatabaseEngine(LiquibaseTestUtil.H2);

        evaluator.setBasePath("changesetDir");


        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getBuildersList().add(evaluator);
        project.setCustomWorkspace(changesetDir.getParent());

        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build, isSuccessful());

        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);

        assertThat(build.getAction(ExecutedChangesetAction.class), CoreMatchers.notNullValue());
        assertThat(action.getSuccessfulChangeSets(), contains(IsChangeSetDetail.hasId("create-table")));
    }

    @Test
    public void should_handle_json_changesets_successfully()
            throws InterruptedException, ExecutionException, IOException {
        FreeStyleBuild build = createAndBuildLiquibaseProject("/example-changesets/json-changeset-sunnyday.json");
        assertThat(build, isSuccessful());
    }

    @Test
    public void should_have_expected_executed_changesets()
            throws InterruptedException, ExecutionException, IOException {

        FreeStyleBuild build = createAndBuildErrorFreeProject();
        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);
        assertThat(action.getChangeSetDetails(), containsSunnyDayChangesetDetails());
        ChangeSetDetail changeSetDetail = action.getChangeSetDetails().get(0);
        assertThat(changeSetDetail.getExecutedSql(), notNullValue());
        assertThat(action.isTagApplied(), is(false));
    }

    @Test
    public void should_handle_changelog_with_include() throws IOException, ExecutionException, InterruptedException {
        File rootDirectory = temporaryFolder.getRoot();
        File changeLog = LiquibaseTestUtil.createFileFromResource(rootDirectory, "/example-changesets/include-one.xml");
        FreeStyleProject project = createProjectWithChangelogFile(changeLog);
        LiquibaseTestUtil.createFileFromResource(rootDirectory, LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML);
        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build, isSuccessful());

        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);

        assertThat(action.getChangeSetDetails(), containsSunnyDayChangesetDetails());
    }

    @Test
    public void should_handle_include_with_absolute_path() throws IOException, ExecutionException,
            InterruptedException {

        File directoryContainingIncludedChangeset = temporaryFolder.newFolder(RandomStringUtils.randomAlphabetic(5));
        File includedChangeset = LiquibaseTestUtil
                .createFileFromResource(directoryContainingIncludedChangeset,
                        LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML);

        String changeLogText = IOUtils.toString(getClass().getResourceAsStream(
                "/example-changesets/include-with-absolute-path.xml"));
        String resolvedText = changeLogText.replaceAll("@PATH@", includedChangeset.getAbsolutePath());
        File rootChangelog = new File(temporaryFolder.getRoot(), "include-with-absolute-path.xml");
        FileUtils.write(rootChangelog, resolvedText);

        FreeStyleProject project = createProjectWithChangelogFile(rootChangelog);

        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build, isSuccessful());
        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);
        assertThat(action.getChangeSetDetails(), containsSunnyDayChangesetDetails());
    }

    @Test
    public void should_handle_include_all() throws IOException, ExecutionException, InterruptedException {
        File rootChangeset =
                LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(),
                        "/example-changesets/include-all-changeset.xml");

        File includedDir = new File(rootChangeset.getParentFile(), "include-all");
        includedDir.mkdirs();
        LiquibaseTestUtil.createFileFromResource(includedDir, LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML);

        FreeStyleProject project = createProjectWithChangelogFile(rootChangeset);
        // use of includeAll means changeset files must reside in project's workspace.
        // here we do so by setting the project's custom workspace to the directory where these files reside.
        // Normally, this would presumably be achieved by checking out the changesets via source control to
        // the project's workspace..
        project.setCustomWorkspace(includedDir.getParent());
        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build, isSuccessful());
        List<ChangeSetDetail> changeSetDetails = build.getAction(ExecutedChangesetAction.class).getChangeSetDetails();

        assertThat(changeSetDetails, containsSunnyDayChangesetDetails());
    }

    @Test
    public void should_apply_tag()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {

        File inmemoryDatabaseFile = temporaryFolder.newFile();
        String dbUrl = "jdbc:h2:file:" + inmemoryDatabaseFile.getAbsolutePath();

        File sunnyDayChangeset = LiquibaseTestUtil.createErrorFreeChangeset(temporaryFolder);
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setChangeLogFile(sunnyDayChangeset.getAbsolutePath());
        evaluator.setUrl(dbUrl);
        evaluator.setDatabaseEngine(LiquibaseTestUtil.H2);
        evaluator.setTagOnSuccessfulBuild(true);
        project.getBuildersList().add(evaluator);

        FreeStyleBuild build = launchBuildForProject(project);

        Properties liquibaseProperties = new Properties();
        liquibaseProperties.load(getClass().getResourceAsStream("/example-changesets/unit-test.h2.liquibase.properties"));
        Connection connection = DriverManager.getConnection(dbUrl, liquibaseProperties);
        DatabaseConnection jdbcConnection = new JdbcConnection(connection);

        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

        Liquibase liquibase = new Liquibase(sunnyDayChangeset.getAbsolutePath(), new FileSystemResourceAccessor(), database);
        boolean tagExists = liquibase.tagExists(project.getName() + "-" + build.getNumber());

        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);

        assertThat(tagExists, is(true));
        assertThat(action.isTagApplied(), is(true));
    }


    @Test
    @Ignore("Possibly not working due to liquibase issue https://liquibase.jira.com/browse/CORE-2761")
    public void should_handle_include_all_relative() throws IOException, ExecutionException, InterruptedException {
        File rootChangeset =
                LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(),
                        "/example-changesets/include-all-changeset.xml");

        File includedDir = new File(rootChangeset.getParentFile(), "include-all");
        includedDir.mkdirs();
        LiquibaseTestUtil.createFileFromResource(includedDir, LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML);

        FreeStyleProject project = createProjectWithChangelogFile(rootChangeset);
        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build, isSuccessful());
        List<ChangeSetDetail> changeSetDetails = build.getAction(ExecutedChangesetAction.class).getChangeSetDetails();

        assertThat(changeSetDetails, containsSunnyDayChangesetDetails());
    }


    protected FreeStyleBuild createAndBuildLiquibaseProject(String changesetResourcePath)
            throws IOException, InterruptedException, ExecutionException {
        File yamlChangeset = LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(), changesetResourcePath);
        FreeStyleProject project = createProjectWithChangelogFile(yamlChangeset);
        return launchBuildForProject(project);
    }

    protected FreeStyleBuild createAndBuildErrorFreeProject()
            throws IOException, InterruptedException, ExecutionException {
        File changelogFile = LiquibaseTestUtil.createErrorFreeChangeset(temporaryFolder);
        FreeStyleProject project = createProjectWithChangelogFile(changelogFile);
        return launchBuildForProject(project);
    }

    /**
     * Matches the changeset order as represented in "sunny-day-changeset.xml"
     *
     * @return
     */
    private static Matcher<Iterable<? extends ChangeSetDetail>> containsSunnyDayChangesetDetails() {
        return  contains(
                isChangeSetDetail(new ChangeSetDetail.Builder().withAuthor("keith").withId("create-table")
                                                               .withComments("This is a simple create table")
                                                               .withSuccessfullyExecuted(true).build()),
                isChangeSetDetail(new ChangeSetDetail.Builder().withAuthor("keith").withId("first_tag").build()),
                hasId("create-color-table"),
                hasId("create-testing-table"));
    }

    protected FreeStyleProject createProjectWithChangelogFile(File changelogFile) throws IOException {
        JenkinsRule jenkinsRule = this.jenkinsRule;
        return LiquibaseTestUtil.createLiquibaseProject(changelogFile, jenkinsRule);
    }

    protected static FreeStyleBuild launchBuildForProject(FreeStyleProject project)
            throws InterruptedException, ExecutionException {
        return project.scheduleBuild2(0).get();
    }
}
