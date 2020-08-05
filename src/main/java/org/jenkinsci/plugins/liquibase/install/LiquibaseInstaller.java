package org.jenkinsci.plugins.liquibase.install;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class LiquibaseInstaller extends DownloadFromUrlInstaller {

    @DataBoundConstructor
    public LiquibaseInstaller(String id) {
        super(id);
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<LiquibaseInstaller> {
        public String getDisplayName() {
            return "Install from Maven Central";
        }

        @Override
        public boolean isApplicable(Class toolType) {
            return toolType==LiquibaseInstallation.class;
        }
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        System.out.println("Installing perform!");
        return super.performInstallation(tool, node, log);
    }
}
