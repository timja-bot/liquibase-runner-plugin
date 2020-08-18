package org.jenkinsci.plugins.liquibase.install;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.liquibase.builder.UpdateBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LiquibaseInstallation extends ToolInstallation implements NodeSpecific<LiquibaseInstallation>, EnvironmentSpecific<LiquibaseInstallation> {

    private static final long serialVersionUID = 1;

    private String liquibaseHome;
    private String databaseDriverUrl;


    @DataBoundConstructor
    public LiquibaseInstallation(String name, String home, String databaseDriverUrl,  List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim("liquibase"), properties);
        liquibaseHome = home;
        this.databaseDriverUrl = databaseDriverUrl;
    }

    @Override
    public LiquibaseInstallation forEnvironment(EnvVars environment) {
        return new LiquibaseInstallation(getName(), environment.expand(liquibaseHome), environment.expand(databaseDriverUrl), getProperties().toList());
    }

    @Override
    public LiquibaseInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new LiquibaseInstallation(getName(), translateFor(node, log), databaseDriverUrl, getProperties().toList());
    }

    @Override
    public String getHome() {
        String resolvedHome;
        if (liquibaseHome != null) {
            resolvedHome= liquibaseHome;
        } else {
            resolvedHome=super.getHome();
        }
        return resolvedHome;
    }


    public File getLiquibaseJar() {
        return new File(liquibaseHome, "liquibase.jar");
    }

    public boolean isValidLiquibaseHome() {
        final File liquibaseJar = getLiquibaseJar();
        return liquibaseJar != null && liquibaseJar.exists();
    }

    public String getDatabaseDriverUrl() {
        return databaseDriverUrl;
    }

    @DataBoundSetter
    public void setDatabaseDriverUrl(String databaseDriverUrl) {
        this.databaseDriverUrl = databaseDriverUrl;
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<LiquibaseInstallation> {

        private LiquibaseInstallation[] installations = new LiquibaseInstallation[0];

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Liquibase";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new LiquibaseInstaller(null));
        }

        public LiquibaseInstallation[] getInstallations() {
            return Arrays.copyOf(installations, installations.length);
        }

        @Override
        public void setInstallations(LiquibaseInstallation... installations) {
            this.installations = installations;
            save();
        }
    }
}
