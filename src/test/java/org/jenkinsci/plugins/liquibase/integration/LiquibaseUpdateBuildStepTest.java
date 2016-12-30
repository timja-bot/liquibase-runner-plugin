package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.Result;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
import static org.junit.Assert.assertThat;


public class LiquibaseUpdateBuildStepTest {
    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseUpdateBuildStepTest.class);

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected String pipelineScript;
    protected WorkflowJob job;

    @Before
    public void setup() throws IOException {
        File workspace = temporaryFolder.newFolder("workspace");
        pipelineScript = generatePipelineScript(workspace);
        copyChangeLogFileToWorkspace(workspace);
        job = jenkinsRule.jenkins.createProject(WorkflowJob.class, RandomStringUtils.randomAlphabetic(8));
    }

    @Test
    public void should_allow_friendly_update_name() throws ExecutionException, InterruptedException {
        job.setDefinition(new CpsFlowDefinition(pipelineScript));
        WorkflowRun workflowRun = job.scheduleBuild2(0).get();
        assertThat(workflowRun.getResult(), is(Result.SUCCESS));
    }

    private static void copyChangeLogFileToWorkspace(File workspace) throws IOException {
        LiquibaseTestUtil.createFileFromResource(workspace, "/example-changesets/sunny-day-changeset.xml");
    }

    private String generatePipelineScript(File workspace) throws IOException {
        String template = IOUtils.toString(getClass().getResourceAsStream("/pipeline-with-ws-token.groovy"));
        return template.replaceAll("@WORKSPACE@", workspace.getAbsolutePath());

    }
}
