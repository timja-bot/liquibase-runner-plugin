package org.jenkinsci.plugins.liquibase.builder;

import hudson.AbortException;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Properties;

/**
 * Jenkins builder which evaluates liquibase changesets.
 */
@SuppressWarnings("ProhibitedExceptionThrown")
public class RawCliBuilder extends AbstractLiquibaseBuilder {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String commandArguments;

    @DataBoundConstructor
    public RawCliBuilder() {
        super();
    }

    @Override
    protected void addCommandAndArguments(ArgumentListBuilder cliCommand, Properties configProperties, Run<?, ?> build, TaskListener listener) throws IOException {
        if (commandArguments == null || commandArguments.trim().equals("")) {
            throw new AbortException("No command line specified in '" + getDescriptor().getDisplayName() + "' configuration");
        }

        cliCommand.add(commandArguments.replaceAll("\r\n", " ").replaceAll("\n", " ").split(" "));
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public String getCommandArguments() {
        return commandArguments;
    }

    @DataBoundSetter
    public void setCommandArguments(String commandArguments) {
        this.commandArguments = commandArguments;
    }

    @Extension
    public static class DescriptorImpl extends AbstractLiquibaseDescriptor {

        public DescriptorImpl() {
            load();
        }

        public DescriptorImpl(Class<? extends RawCliBuilder> clazz) {
            super(clazz);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Liquibase: CLI Command";
        }

    }

}
