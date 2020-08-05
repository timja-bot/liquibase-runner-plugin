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
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;

public class LiquibaseInstallation extends ToolInstallation implements NodeSpecific<LiquibaseInstallation>, EnvironmentSpecific<LiquibaseInstallation> {

    private static final long serialVersionUID = 1;

    private String liquibaseHome;


    @DataBoundConstructor
    public LiquibaseInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim("liquibase"), properties);
        liquibaseHome = home;
    }

    @Override
    public LiquibaseInstallation forEnvironment(EnvVars environment) {
        return new LiquibaseInstallation(getName(), environment.expand(liquibaseHome), getProperties().toList());
    }

    @Override
    public LiquibaseInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new LiquibaseInstallation(getName(), translateFor(node, log), getProperties().toList());
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

    public ClassLoader getClassLoader() {
        URLClassLoader urlClassLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                try {
                    return new URLClassLoader(new URL[]{getLiquibaseJar().toURI().toURL()});
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        return urlClassLoader;

    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<LiquibaseInstallation> {

        @Override
        public String getDisplayName() {
            return "Liquibase";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new LiquibaseInstaller(null));
        }

        public LiquibaseInstallation[] getInstallations() {
            return Jenkins.get().getDescriptorByType(ChangesetEvaluator.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(LiquibaseInstallation... installations) {
            Jenkins.get().getDescriptorByType(ChangesetEvaluator.DescriptorImpl.class).setInstallations(installations);
        }
    }
}
