package org.jenkinsci.plugins.liquibase.installation;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.jenkinsci.plugins.liquibase.builder.LiquibaseBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Describes details of liquibase installation.
 */
public class LiquibaseInstallation extends ToolInstallation
        implements NodeSpecific<LiquibaseInstallation>, EnvironmentSpecific<LiquibaseInstallation>, Serializable {

    public static final String UNIX_EXEC_NAME = "liquibase";
    public static final String WINDOWS_EXEC_NAME = "liquibase.bat";

    @DataBoundConstructor
    public LiquibaseInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
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
            return Jenkins.getInstance().getDescriptorByType(LiquibaseBuilder.DESCRIPTOR.getClass()).getInstallations();
        }

        @Override
        public void setInstallations(LiquibaseInstallation... installations) {
            Jenkins.getInstance().getDescriptorByType(LiquibaseBuilder.DESCRIPTOR.getClass())
                   .setInstallations(installations);
        }
    }
}
