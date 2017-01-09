package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.model.Project;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class CredentialsMigratorTest {


    private static final Logger LOG = LoggerFactory.getLogger(CredentialsMigratorTest.class);

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule().withPresetData("update-test-data");

    /**
     * Covers JENKINS-40920.  The preset data contains a job containing < 1.2.0 plugin format, and
     * whose credentials should therefore be migrated.
     * The test does verify that the old job does indeed exist in the preset data.
     * @throws IOException
     */
    @Test
    public void should_migrate_both_builder() throws IOException {
        Jenkins jenkins = jenkinsRule.getInstance();
        List<Project> projects = jenkins.getItems(Project.class);

        boolean presetDataIncludedLiquibaseBuilder = false;
        boolean projectContainedMoreThanOneBuilder = false;

        int builderCounter = 0;

        for (Project project : projects) {
            List all = project.getBuildersList().getAll(AbstractLiquibaseBuilder.class);
            for (Object o : all) {
                AbstractLiquibaseBuilder liquibaseBuilder = (AbstractLiquibaseBuilder) o;
                builderCounter++;
                if (liquibaseBuilder!=null) {
                    assertThat(liquibaseBuilder.getCredentialsId(), is(notNullValue()));
                    assertThat(liquibaseBuilder.hasLegacyCredentials(), is(false));
                    if (!presetDataIncludedLiquibaseBuilder) {
                        presetDataIncludedLiquibaseBuilder = true;
                    }
                }
            }
            projectContainedMoreThanOneBuilder = builderCounter>1;
        }

        assertThat(presetDataIncludedLiquibaseBuilder, is(true));
        assertThat(projectContainedMoreThanOneBuilder, is(true));
    }

}