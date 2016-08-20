package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.hamcrest.Matcher;
import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.ExecutedChangesetAction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.jenkinsci.plugins.liquibase.integration.IsChangesetDetailWithId.changeSetDetailWithId;
import static org.junit.Assert.assertThat;

public class ChangesetEvaluatorBuildResultTest {

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluatorBuildResultTest.class);
    private static final String LIQUIBASE_PROPERTIES = "/example-changesets/h2.liquibase.properties";
    private static final int NUMBER_OF_CHANGESETS = 4;

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
                .createProjectFiles(temporaryFolder, LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML, LIQUIBASE_PROPERTIES);
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.setCustomWorkspace(temporaryFolder.getRoot().getAbsolutePath());
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setLiquibasePropertiesPath(LiquibaseTestUtil.extractFilenameFromResourcePath(LIQUIBASE_PROPERTIES));
        project.getBuildersList().add(evaluator);
        FreeStyleBuild build = launchBuildForProject(project);
        assertThat(build.getResult(), is(Result.SUCCESS));
    }


    @Test
    public void should_executed_json_changeset_sucessfully()
            throws InterruptedException, ExecutionException, IOException {
        FreeStyleBuild build = createAndBuildLiquibaseProject("/example-changesets/json-changeset-sunnyday.json");
        assertThat(build.getResult(), is(Result.SUCCESS));
    }

    @Test
    public void should_have_expected_executed_changesets()
            throws InterruptedException, ExecutionException, IOException {

        FreeStyleBuild build = createAndBuildErrorFreeProject();
        ExecutedChangesetAction action = build.getActions(ExecutedChangesetAction.class).get(0);
        assertThat(action.getChangeSetDetails(), containsSunnyDayChangesetDetails());
    }

    @Test
    public void should_handle_changelog_with_include() throws IOException, ExecutionException, InterruptedException {
        File changeLog = LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(),
                "/example-changesets/include-one.xml");
        FreeStyleProject project = createProjectWithChangelogFile(changeLog);
        LiquibaseTestUtil.createProjectFiles(temporaryFolder, "/example-changesets/sunny-day-changeset.xml");
        FreeStyleBuild build = launchBuildForProject(project);

        assertThat(build.getResult(), is(Result.SUCCESS));
        ExecutedChangesetAction action = build.getAction(ExecutedChangesetAction.class);

        assertThat(action.getChangeSetDetails(), hasSize(NUMBER_OF_CHANGESETS));
        assertThat(action.getChangeSetDetails(), containsSunnyDayChangesetDetails());
    }


    @Test
    public void should_handle_include_all() throws IOException, ExecutionException, InterruptedException {
        File rootChangeset =
                LiquibaseTestUtil.createFileFromResource(temporaryFolder.getRoot(),
                        "/example-changesets/include-all-changeset.xml");

        File parentFile = rootChangeset.getParentFile();

        File includedDir = new File(parentFile, "include-all");
        includedDir.mkdirs();

        LiquibaseTestUtil.createFileFromResource(includedDir, "/example-changesets/sunny-day-changeset.xml");

        FreeStyleProject projectWithChangelogFile = createProjectWithChangelogFile(rootChangeset);
        projectWithChangelogFile.setCustomWorkspace(includedDir.getParent());
        FreeStyleBuild freeStyleBuild = launchBuildForProject(projectWithChangelogFile);


        assertThat(freeStyleBuild.getResult(), is(Result.SUCCESS));
        List<ChangeSetDetail> changeSetDetails =
                freeStyleBuild.getAction(ExecutedChangesetAction.class).getChangeSetDetails();

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

    private static Matcher<Iterable<? extends ChangeSetDetail>> containsSunnyDayChangesetDetails() {
        return contains(
                changeSetDetailWithId("create-table"),
                changeSetDetailWithId("first_tag"),
                changeSetDetailWithId("create-color-table"),
                changeSetDetailWithId("create-testing-table"));
    }

    protected FreeStyleProject createProjectWithChangelogFile(File changelogFile) throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setChangeLogFile(changelogFile.getAbsolutePath());
        evaluator.setUrl(LiquibaseTestUtil.IN_MEMORY_JDBC_URL);
        evaluator.setDatabaseEngine("H2");
        project.getBuildersList().add(evaluator);
        return project;
    }

    protected static FreeStyleBuild launchBuildForProject(FreeStyleProject project)
            throws InterruptedException, ExecutionException {
        return project.scheduleBuild2(0).get();
    }

}
