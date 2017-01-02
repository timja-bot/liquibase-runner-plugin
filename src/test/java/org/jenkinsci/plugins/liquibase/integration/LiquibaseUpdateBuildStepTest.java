package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.Result;
import liquibase.exception.LiquibaseException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jenkinsci.plugins.liquibase.evaluator.RolledbackChangesetAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;


public class LiquibaseUpdateBuildStepTest {
    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseUpdateBuildStepTest.class);

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

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
        assertThat(workflowRun.getResult(), is(Result.SUCCESS));
    }

    @Test
    public void should_allow_rollback_dsl()
            throws IOException, SQLException, LiquibaseException, ExecutionException, InterruptedException {
        File databaseFile = temporaryFolder.newFile();
        String jdbcUrl = LiquibaseTestUtil.composeJdbcUrl(databaseFile);
        String baseScript = generatePipelineScript(workspace, "/rollback-pipeline-template.groovy");
        String script = baseScript.replaceAll("@DB_URL@", jdbcUrl);
        CpsFlowDefinition cpsFlowDefinition = new CpsFlowDefinition(script);
        job.setDefinition(cpsFlowDefinition);
        WorkflowRun run = job.scheduleBuild2(0).get();
        assertThat(run.getResult(), is(Result.SUCCESS));
        RolledbackChangesetAction action = run.getAction(RolledbackChangesetAction.class);
        assertThat(action.getRolledbackChangesets().size(), not(0));
    }

    private static File copyChangeLogFileToWorkspace(File workspace) throws IOException {
        return LiquibaseTestUtil.createFileFromResource(workspace, "/example-changesets/sunny-day-changeset.xml");
    }

    private String generateUpdatePipelineScript(File workspace) throws IOException {
        String resourcePath = "/pipeline-with-ws-token.groovy";
        return generatePipelineScript(workspace, resourcePath);
    }

    private String generatePipelineScript(File workspace, String resourcePath) throws IOException {
        String template = IOUtils.toString(getClass().getResourceAsStream(resourcePath));
        return template.replaceAll("@WORKSPACE@", workspace.getAbsolutePath());
    }
}
