package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.ExecutedChangesetAction;
import org.junit.Before;
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
import static org.jenkinsci.plugins.liquibase.integration.ChangesetDetailMatcher.isChangeSetDetail;
import static org.jenkinsci.plugins.liquibase.integration.ChangesetDetailMatcher.isChangesetWithId;
import static org.junit.Assert.assertThat;

public class ChangesetEvaluatorBuildResultTest {

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluatorBuildResultTest.class);
    private static final String LIQUIBASE_PROPERTIES = "/example-changesets/h2.liquibase.properties";
    private static final String H2 = "H2";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

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
        assertThat(freeStyleBuild.getResult(), is(Result.SUCCESS));
    }

    @Test
    public void should_indicate_unstable_build_when_changeset_has_error()
            throws IOException, ExecutionException, InterruptedException {
        File changesetFileWithError = LiquibaseTestUtil.createChangesetFileWithError(temporaryFolder);
        FreeStyleProject project = createProjectWithChangelogFile(changesetFileWithError);
        FreeStyleBuild build = launchBuildForProject(project);
        assertThat(build.getResult(), is(Result.UNSTABLE));
    }

    @Test
    public void should_indicate_success_with_yaml_formatted_changeset()
            throws IOException, ExecutionException, InterruptedException {
        FreeStyleBuild build = createAndBuildLiquibaseProject("/example-changesets/yaml-changeset-sunnyday.yml");
        assertThat(build.getResult(), is(Result.SUCCESS));
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
        assertThat(build.getResult(), is(Result.SUCCESS));
    }


    @Test
    public void should_handle_json_changesets_successfully()
            throws InterruptedException, ExecutionException, IOException {
        FreeStyleBuild build = createAndBuildLiquibaseProject("/example-changesets/json-changeset-sunnyday.json");
        assertThat(build.getResult(), is(Result.SUCCESS));
    }

    @Test
    public void should_have_expected_executed_changesets()
            throws InterruptedException, ExecutionException, IOException {

        FreeStyleBuild build = createAndBuildErrorFreeProject();
        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);
        assertThat(action.getChangeSetDetails(), containsSunnyDayChangesetDetails());
        ChangeSetDetail changeSetDetail = action.getChangeSetDetails().get(0);
        assertThat(changeSetDetail.getExecutedSql(), notNullValue());
    }

    @Test
    public void should_handle_changelog_with_include() throws IOException, ExecutionException, InterruptedException {
        File rootDirectory = temporaryFolder.getRoot();
        File changeLog = LiquibaseTestUtil.createFileFromResource(rootDirectory, "/example-changesets/include-one.xml");
        FreeStyleProject project = createProjectWithChangelogFile(changeLog);
        LiquibaseTestUtil.createFileFromResource(rootDirectory, LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML);
        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build.getResult(), is(Result.SUCCESS));
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

        assertThat(build.getResult(), is(Result.SUCCESS));
        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);
        assertThat(action.getChangeSetDetails(), containsSunnyDayChangesetDetails());
    }

    @Test
    @Ignore("Pending my understanding of how the relativeToChangelogFile attribute is supposed to work with includeAll")
    public void should_handle_include_all_relative() throws IOException, ExecutionException, InterruptedException {
        File rootChangeset =
                LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(),
                        "/example-changesets/include-all-changeset.xml");

        File includedDir = new File(rootChangeset.getParentFile(), "include-all");
        includedDir.mkdirs();
        LiquibaseTestUtil.createFileFromResource(includedDir, "/example-changesets/sunny-day-changeset.xml");

        FreeStyleProject project = createProjectWithChangelogFile(rootChangeset);
        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build.getResult(), is(Result.SUCCESS));
        List<ChangeSetDetail> changeSetDetails = build.getAction(ExecutedChangesetAction.class).getChangeSetDetails();

        assertThat(changeSetDetails, containsSunnyDayChangesetDetails());
    }


    @Test
    public void should_handle_include_all() throws IOException, ExecutionException, InterruptedException {
        File rootChangeset =
                LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(),
                        "/example-changesets/include-all-changeset.xml");

        File includedDir = new File(rootChangeset.getParentFile(), "include-all");
        includedDir.mkdirs();
        LiquibaseTestUtil.createFileFromResource(includedDir, "/example-changesets/sunny-day-changeset.xml");

        FreeStyleProject project = createProjectWithChangelogFile(rootChangeset);
        // use of includeAll means changeset files must reside in project's workspace.
        // here we do so by setting the project's custom workspace to the directory where these files reside.
        // Normally, this would presumably be achieved by checking out the changesets via source control.
        project.setCustomWorkspace(includedDir.getParent());
        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build.getResult(), is(Result.SUCCESS));
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
        return contains(
                isChangeSetDetail(new ChangeSetDetail.Builder().withAuthor("keith").withId("create-table")
                                                               .withComments("This is a simple create table")
                                                               .withSuccessfullyExecuted(true).build()),
                isChangeSetDetail(new ChangeSetDetail.Builder().withAuthor("keith").withId("first_tag").build()),
                isChangesetWithId("create-color-table"),
                isChangesetWithId("create-testing-table"));
    }

    protected FreeStyleProject createProjectWithChangelogFile(File changelogFile) throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setChangeLogFile(changelogFile.getAbsolutePath());
        evaluator.setUrl(LiquibaseTestUtil.IN_MEMORY_JDBC_URL);
        evaluator.setDatabaseEngine(H2);
        project.getBuildersList().add(evaluator);
        return project;
    }

    protected static FreeStyleBuild launchBuildForProject(FreeStyleProject project)
            throws InterruptedException, ExecutionException {
        return project.scheduleBuild2(0).get();
    }

}
