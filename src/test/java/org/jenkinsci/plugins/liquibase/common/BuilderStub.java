package org.jenkinsci.plugins.liquibase.common;

import hudson.EnvVars;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.liquibase.builder.AbstractLiquibaseBuilder;

import java.io.IOException;
import java.util.Properties;

public class BuilderStub extends AbstractLiquibaseBuilder {

    @Override
    protected void addCommandAndArguments(ArgumentListBuilder cliCommand, Properties configProperties, Run<?, ?> build, EnvVars environment, TaskListener listener) throws IOException {

    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return null;
    }

    @Override
    public String getChangeLogFile() {
        return "${token}";
    }
}
