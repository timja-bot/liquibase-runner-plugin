package org.jenkinsci.plugins.liquibase.common;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.integration.LiquibaseTestUtil;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CredentialsTest {

    private static final String DESCRIPTION = "sample";
    @Mock
    private AbstractBuild build;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();
    protected BuilderStub builderStub = new BuilderStub();
    protected String username = RandomStringUtils.randomAlphabetic(10);
    protected String password = RandomStringUtils.randomAlphabetic(10);
    protected String credentialsId = RandomStringUtils.randomAlphabetic(4);


    @Before
    public void setup() throws IOException {
        temporaryFolder.create();

        UsernamePasswordCredentialsImpl credentials =
                new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, DESCRIPTION, username,
                        password);
        CredentialsProvider.lookupStores(jenkinsRule.getInstance()).iterator().next()
                           .addCredentials(Domain.global(), credentials);

    }

    @Test
    public void should_use_credentials() throws IOException, InterruptedException {
        builderStub.setCredentialsId(credentialsId);
        Properties properties = new Properties();
        PropertiesAssembler.assembleFromProjectConfiguration(builderStub, properties, new EnvVars(), build);

        String actualUsername = getProperty(properties, LiquibaseProperty.USERNAME);
        String actualPassword = getProperty(properties, LiquibaseProperty.PASSWORD);

        assertThat(actualUsername, is(username));
        assertThat(actualPassword, is(password));

    }

    @Test
    public void should_prefer_credentials_defined_in_credentials_config()
            throws IOException, InterruptedException, ExecutionException {

        File propertiesfile = temporaryFolder.newFile("liquibase.properties");
        FileUtils.write(propertiesfile, "username=i_shouldnt_be_used\n");

        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setChangeLogFile(LiquibaseTestUtil
                .createFileFromResource(temporaryFolder.getRoot(), "/example-changesets/single-changeset.xml")
                .getAbsolutePath());
        evaluator.setUrl(LiquibaseTestUtil.IN_MEMORY_JDBC_URL);
        evaluator.setDatabaseEngine(LiquibaseTestUtil.H2);
        evaluator.setLiquibasePropertiesPath(propertiesfile.getAbsolutePath());
        evaluator.setCredentialsId(credentialsId);
        project.getBuildersList().add(evaluator);

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        Properties liquibaseProperties =
                PropertiesAssembler.createLiquibaseProperties(evaluator, build, new EnvVars(), build.getWorkspace());

        String resolvedUsername = getProperty(liquibaseProperties, LiquibaseProperty.USERNAME);
        String resolvedPassword = getProperty(liquibaseProperties, LiquibaseProperty.PASSWORD);

        assertThat(resolvedUsername, is(username));
        assertThat(resolvedPassword, is(password));

    }

    private static String getProperty(Properties properties, LiquibaseProperty propertyName) {
        return properties.getProperty(propertyName.propertyName());
    }
}
