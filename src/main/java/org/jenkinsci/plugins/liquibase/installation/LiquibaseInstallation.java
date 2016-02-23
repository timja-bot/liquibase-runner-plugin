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
import net.sf.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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

    public static LiquibaseInstallation[] allInstallations() {
        LiquibaseInstallation.DescriptorImpl descriptorByType = Jenkins.getInstance().getDescriptorByType(LiquibaseInstallation.DescriptorImpl.class);
        return descriptorByType.getInstallations();
    }

    public static LiquibaseInstallation getInstallation(String name) {
        LiquibaseInstallation[] liquibaseInstallations = allInstallations();
        LiquibaseInstallation found = null;
        for (int i = 0; i < liquibaseInstallations.length; i++) {
            LiquibaseInstallation liquibaseInstallation = liquibaseInstallations[i];
            boolean equals = liquibaseInstallation.getName().equals(name);
            if (equals) {
                found = liquibaseInstallation;
                break;
            }
        }
        if (found == null) {
            throw new RuntimeException("Liquibase installation with name '" + name + "' was not found");
        }

        return found;
    }


    @Extension
    public static class DescriptorImpl extends ToolDescriptor<LiquibaseInstallation> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Liquibase";
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            super.configure(req, json);
            save();
            return true;
        }

    }
}
