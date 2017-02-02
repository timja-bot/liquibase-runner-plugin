package org.jenkinsci.plugins.liquibase.integration;

import liquibase.exception.LiquibaseException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.StringContains;
import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;
import org.jenkinsci.plugins.liquibase.evaluator.ExecutedChangesetAction;
import org.jenkinsci.plugins.liquibase.evaluator.RolledbackChangesetAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.jenkinsci.plugins.liquibase.matchers.BuildResultMatcher.isSuccessful;
import static org.junit.Assert.assertThat;


public class LiquibasePipelineTest {
    private static final Logger LOG = LoggerFactory.getLogger(LiquibasePipelineTest.class);

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected WorkflowJob job;
    protected File changeLogFile;
    protected File workspace;

    @Before
    public void setup() throws IOException {
        workspace = temporaryFolder.newFolder("workspace");
        changeLogFile = copyChangeLogFileToWorkspace(workspace);
        job = jenkinsRule.jenkins.createProject(WorkflowJob.class, RandomStringUtils.randomAlphabetic(8));
    }

    @Test
    public void should_allow_friendly_update_name() throws ExecutionException, InterruptedException, IOException {
        String updateScript = generateUpdatePipelineScript(workspace);
        job.setDefinition(new CpsFlowDefinition(updateScript));
        WorkflowRun workflowRun = job.scheduleBuild2(0).get();
        assertThat(workflowRun, isSuccessful());

    }

    @Test
    public void should_allow_dbdoc_generation() throws IOException, ExecutionException, InterruptedException {
        String script = generatePipelineScript(workspace, "/pipeline-scripts/db-doc-template.groovy");
        LiquibaseTestUtil.createFileFromResource(workspace, LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML);
        job.setDefinition(new CpsFlowDefinition(script));
        WorkflowRun workflowRun = job.scheduleBuild2(0).get();

        assertThat(workflowRun, isSuccessful());

        File reportIndex = new File(new File(workspace, "doc"), "index.html");

        assertThat(reportIndex, exists());

    }
    @Test
    public void should_allow_changelog_parameters() throws IOException, ExecutionException, InterruptedException {
        String baseScript = generatePipelineScript(workspace, "/pipeline-scripts/pipeline-with-changelog-params.groovy");
        String parameterValue = RandomStringUtils.randomAlphabetic(5);
        String pipelineScript = baseScript.replaceAll("@PARAM_VALUE@", parameterValue);
        job.setDefinition(new CpsFlowDefinition(pipelineScript));
        LiquibaseTestUtil.createFileFromResource(workspace, "/example-changesets/with-changelog-property.xml");
        WorkflowRun workflowRun = job.scheduleBuild2(0).get();

        assertThat(workflowRun, isSuccessful());
        ExecutedChangesetAction action = workflowRun.getAction(ExecutedChangesetAction.class);
        assertThat(action, notNullValue());
        List<ChangeSetDetail> changeSetDetails = action.getChangeSetDetails();
        assertThat(changeSetDetails, hasSize(1));
        String executedSql = changeSetDetails.get(0).getExecutedSql();
        assertThat(executedSql, StringContains.containsString(parameterValue));

    }

    @Test
    public void should_allow_rollback_dsl()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {
        File databaseFile = temporaryFolder.newFile();
        String jdbcUrl = LiquibaseTestUtil.composeJdbcUrl(databaseFile);
        String baseScript = generatePipelineScript(workspace, "/pipeline-scripts/rollback-pipeline-template.groovy");
        String script = baseScript.replaceAll("@DB_URL@", jdbcUrl);
        CpsFlowDefinition cpsFlowDefinition = new CpsFlowDefinition(script);
        job.setDefinition(cpsFlowDefinition);
        WorkflowRun run = job.scheduleBuild2(0).get();
        assertThat(run, isSuccessful());
        RolledbackChangesetAction action = run.getAction(RolledbackChangesetAction.class);
        assertThat(action.getRolledbackChangesets().size(), not(0));
    }

    private static File copyChangeLogFileToWorkspace(File workspace) throws IOException {
        String changesetResourcePath = LiquibaseTestUtil.SUNNY_DAY_CHANGESET_XML;
        return LiquibaseTestUtil.createFileFromResource(workspace, changesetResourcePath);
    }

    private String generateUpdatePipelineScript(File workspace) throws IOException {
        return generatePipelineScript(workspace, "/pipeline-scripts/pipeline-template.groovy");
    }

    private String generatePipelineScript(File workspace, String resourcePath) throws IOException {
        String template = IOUtils.toString(getClass().getResourceAsStream(resourcePath));
        return template.replaceAll("@WORKSPACE@", workspace.getAbsolutePath());
    }

    private static Matcher<File> exists() {
        return new TypeSafeMatcher<File>() {
            @Override
            protected boolean matchesSafely(File item) {
                return item.exists();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("file which exists");
            }
        };
    }
}
