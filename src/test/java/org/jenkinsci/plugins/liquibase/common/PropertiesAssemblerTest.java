package org.jenkinsci.plugins.liquibase.common;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesAssemblerTest {

    @Mock
    private AbstractBuild build;
    @Mock
    private BuildListener listener;
    protected Properties properties = new Properties();
    protected LiquibaseProperty changelogFile = LiquibaseProperty.CHANGELOG_FILE;
    protected EnvVars envVars = new EnvVars();


    @Test
    public void should_resolve_env_variables() throws IOException, InterruptedException {
        String environmentProperty = "token";
        String environmentValue = "resolvedValue";
        envVars.put(environmentProperty, environmentValue);

        when(build.getEnvironment(listener)).thenReturn(envVars);

        String expression = "${" + environmentProperty + "}";
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
        PropertiesAssembler.addPropertyIfDefined(properties, changelogFile, value, envVars);

        assertThat(properties.getProperty(changelogFile.propertyName()), is(value));
    }


}