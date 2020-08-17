package org.jenkinsci.plugins.liquibase.builder;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Properties;

/**
 * Jenkins builder which evaluates liquibase changesets.
 */
@SuppressWarnings("ProhibitedExceptionThrown")
public class DropAllBuilder extends AbstractLiquibaseBuilder {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public DropAllBuilder() {
        super();
    }

    @Override
    protected void addCommandAndArguments(ArgumentListBuilder cliCommand, Properties configProperties, Run<?, ?> build, EnvVars environment, TaskListener listener) {
        cliCommand.add("dropAll");
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static class DescriptorImpl extends AbstractLiquibaseDescriptor {

        public DescriptorImpl() {
            load();
        }

        public DescriptorImpl(Class<? extends DropAllBuilder> clazz) {
            super(clazz);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Liquibase: Drop everything in database";
        }
    }

}
