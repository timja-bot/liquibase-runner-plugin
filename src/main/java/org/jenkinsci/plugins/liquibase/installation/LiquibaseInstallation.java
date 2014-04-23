package org.jenkinsci.plugins.liquibase.installation;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;

import java.io.IOException;
import java.util.List;

import org.jenkinsci.plugins.liquibase.builder.LiquibaseStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Describes details of liquibase installation.
 */
public class LiquibaseInstallation extends ToolInstallation implements NodeSpecific<LiquibaseInstallation>,
        EnvironmentSpecific<LiquibaseInstallation> {
    @DataBoundConstructor
    public LiquibaseInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    }

    public LiquibaseInstallation forEnvironment(EnvVars environment) {
        return new LiquibaseInstallation(getName(), getHome(), getProperties().toList());
    }

    public LiquibaseInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new LiquibaseInstallation(getName(), getHome(), getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<LiquibaseInstallation> {
        @Override
        public String getDisplayName() {
            return "Liquibase";
        }

        @Override
        public LiquibaseInstallation[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(LiquibaseStepDescriptor.class).getInstallations();
        }

        @Override
        public void setInstallations(LiquibaseInstallation... installations) {
            Hudson.getInstance().getDescriptorByType(LiquibaseStepDescriptor.class).setInstallations(installations);
        }
    }
}
