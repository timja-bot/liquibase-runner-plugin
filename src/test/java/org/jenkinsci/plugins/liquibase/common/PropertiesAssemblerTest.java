package org.jenkinsci.plugins.liquibase.common;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesAssemblerTest {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesAssemblerTest.class);

    @Mock
    private AbstractBuild build;

    @Mock
    private Run jobRun;

    @Mock
    private BuildListener listener;
    protected Properties properties = new Properties();
    protected LiquibaseProperty changelogFile = LiquibaseProperty.CHANGELOG_FILE;
    protected EnvVars envVars = new EnvVars();
    protected String environmentProperty = "token";
    protected String environmentValue = "resolvedValue";
    protected String expression;

    @Before
    public void setup() throws IOException {
        expression = "${" + environmentProperty + "}";
    }

    @Test
    public void should_resolve_env_variables() throws IOException, InterruptedException {
        envVars.put(environmentProperty, environmentValue);

        when(build.getEnvironment(listener)).thenReturn(envVars);

        BuilderStub liquibaseBuilder = new BuilderStub();
        liquibaseBuilder.setChangeLogFile(expression);
        PropertiesAssembler.assembleFromProjectConfiguration(liquibaseBuilder, properties,
                build.getEnvironment(listener), build);

        assertThat(properties.getProperty(changelogFile.propertyName()), is(environmentValue));
    }

    @Test
    public void should_not_resolve_expression() {
        Properties properties = new Properties();
        String value = "${not_in_env}";
        PropertiesAssembler.addPropertyIfDefined(properties, changelogFile, value, envVars, build);
        assertThat(properties.getProperty(changelogFile.propertyName()), is(value));
    }

    @Test
    public void should_leave_tokens_alone_when_pipeline_step() throws IOException, InterruptedException {
        envVars.put(environmentProperty, environmentValue);

        when(jobRun.getEnvironment(listener)).thenReturn(envVars);
        PropertiesAssembler.assembleFromProjectConfiguration(new BuilderStub(), properties,
                build.getEnvironment(listener), jobRun);
        assertThat(properties.getProperty(changelogFile.propertyName()), is(expression));
    }
}