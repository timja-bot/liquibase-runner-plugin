package org.jenkinsci.plugins.liquibase.dsl;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import javaposse.jobdsl.plugin.ExecuteDslScripts;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.core.StringContains;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.DatabaseDocBuilder;
import org.jenkinsci.plugins.liquibase.evaluator.RollbackBuilder;
import org.jenkinsci.plugins.liquibase.integration.LiquibaseTestUtil;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.jenkinsci.plugins.liquibase.matchers.BuildResultMatcher.isSuccessful;
import static org.jenkinsci.plugins.liquibase.matchers.ProjectNameMatcher.isProjectWithName;
import static org.junit.Assert.assertThat;

public class LiquibaseRunnerDslExtensionTest {

    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseRunnerDslExtensionTest.class);

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    protected FreeStyleProject project;
    protected File workspace;
    protected String generatedJobName;

    @Before
    public void setup() throws IOException {
        project = jenkinsRule.createFreeStyleProject(RandomStringUtils.randomAlphabetic(5));
        workspace = temporaryFolder.newFolder(RandomStringUtils.randomAlphabetic(8));

        generatedJobName = RandomStringUtils.randomAlphabetic(10);
    }

    @Test
    public void should_spawn_liquibase_project() throws IOException, ExecutionException, InterruptedException {
        FreeStyleBuild build = launchDslProject(generatedJobName, "/dsl/liquibase-update.groovy");

        LOG.debug("build log:{}", formatLogForLog(build.getLog(1000)));

        assertThat(build, isSuccessful());

        List<AbstractProject> projects = jenkinsRule.getInstance().getItems(AbstractProject.class);

        assertThat(projects, hasItem(isProjectWithName(generatedJobName)));
    }

    @Test
    public void should_handle_changelog_parameter_syntax()
            throws InterruptedException, ExecutionException, IOException {
        FreeStyleBuild freeStyleBuild = launchDslProject(generatedJobName, "/dsl/update-with-params.groovy");
        logBuildLog(freeStyleBuild);
        Project project = findGeneratedProject(generatedJobName);
        ChangesetEvaluator changesetEvaluator =
                (ChangesetEvaluator) project.getBuildersList().getAll(ChangesetEvaluator.class).get(0);

        String changeLogParameters = changesetEvaluator.getChangeLogParameters();
        assertThat(changeLogParameters, StringContains.containsString("sample.table.name=blue"));

    }

    @Test
    public void should_build_generated_update_project_successfully()
            throws InterruptedException, ExecutionException, IOException {

        launchDslProject(generatedJobName, "/dsl/liquibase-update.groovy");
        AbstractProject first = findGeneratedProject(generatedJobName);

        assertThat(first, notNullValue());

        LiquibaseTestUtil.createFileFromResource(workspace, "/example-changesets/sunny-day-changeset.xml");

        FreeStyleBuild build = ((FreeStyleProject) first).scheduleBuild2(0).get();

        logBuildLog(build);

        assertThat(build, isSuccessful());

    }

    @Test
    public void should_generate_dbdoc_project() throws InterruptedException, ExecutionException, IOException {
        launchDslProject(generatedJobName, "/dsl/dbdoc.groovy");
        Project project = findGeneratedProject(generatedJobName);
        assertThat(project, notNullValue());
        DatabaseDocBuilder builder =
                (DatabaseDocBuilder) project.getBuildersList().getAll(DatabaseDocBuilder.class).get(0);

        assertThat(builder, notNullValue());
        assertThat(builder.getOutputDirectory(), is("dbdoc"));

    }

    @Test
    public void should_generate_rollback_project() throws InterruptedException, ExecutionException, IOException {
        FreeStyleBuild build = launchDslProject(generatedJobName, "/dsl/rollback-dsl.groovy");
        LOG.debug("build log:{}", formatLogForLog(build.getLog(100)));

        List<AbstractProject> projects = jenkinsRule.getInstance().getItems(AbstractProject.class);

        assertThat(projects, hasItem(isProjectWithName(generatedJobName)));

        Project project = findGeneratedProject(generatedJobName);
        RollbackBuilder rollbackBuilder =
                (RollbackBuilder) project.getBuildersList().getAll(RollbackBuilder.class).get(0);

        assertThat(rollbackBuilder.getNumberOfChangesetsToRollback(), is("2"));
        assertThat(rollbackBuilder.getRollbackToTag(), is("tag"));
        assertThat(rollbackBuilder.getRollbackToDate(), is("13/10/1973 8:00"));
        assertThat(rollbackBuilder.getRollbackLastHours(), is("1"));


    }

    private static void logBuildLog(FreeStyleBuild build) throws IOException {
        LOG.debug("build log of generated project:{}", formatLogForLog(build.getLog(1000)));
    }

    private static Project findGeneratedProject(final String jobName) {
        List<AbstractProject> projects = jenkinsRule.getInstance().getItems(AbstractProject.class);
        Collection<AbstractProject>
                foundGeneratedProjects = Collections2.filter(projects, new Predicate<AbstractProject>() {
            @Override
            public boolean apply(@Nullable AbstractProject abstractProject) {
                boolean include =false;

                if (abstractProject != null) {
                    if (abstractProject.getDisplayName().equals(jobName)) {
                        include = true;
                    } else {
                        include = false;
                    }
                }
                return include;
            }
        });

        return (Project) Iterables.getFirst(foundGeneratedProjects, null);
    }

    private FreeStyleBuild launchDslProject(String jobName, String scriptResourcePath)
            throws IOException, InterruptedException, ExecutionException {
        String scriptTemplate = getScript(scriptResourcePath);
        String script = scriptTemplate.replaceAll("@WORKSPACE@", workspace.getAbsolutePath());
        String resolvedScript = script.replaceAll("@JOB_NAME@", jobName);

        ExecuteDslScripts executeDslScripts = new ExecuteDslScripts();
        executeDslScripts.setScriptText(resolvedScript);

        project.getBuildersList().add(executeDslScripts);

        return project.scheduleBuild2(0).get();
    }

    private String getScript(String resourcePath) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(resourcePath));
    }

    static String formatLogForLog(Iterable<String> buildLog) {
        StringBuilder sb = new StringBuilder();
        for (String line : buildLog) {
            sb.append(line).append("\n");
        }
        return sb.substring(0, sb.length() - 1);
    }

}