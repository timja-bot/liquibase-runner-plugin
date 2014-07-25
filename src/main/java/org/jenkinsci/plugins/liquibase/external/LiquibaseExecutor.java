package org.jenkinsci.plugins.liquibase.external;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.jenkinsci.plugins.liquibase.common.AbstractLiquibaseBuildStep;
import org.jenkinsci.plugins.liquibase.common.LiquibaseProperty;
import org.jenkinsci.plugins.liquibase.installation.LiquibaseInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Strings;

public class LiquibaseExecutor extends AbstractLiquibaseBuildStep {
    public static final String DEFAULT_LOG_LEVEL = "info";
    protected String databaseEngine;
    private String installationName;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String command;



    @DataBoundConstructor
    public LiquibaseExecutor(String url,
                             String password,
                             String changeLogFile,
                             String username,
                             String defaultSchemaName,
                             String liquibasePropertiesPath,
                             boolean testRollbacks,
                             String contexts,
                             String databaseEngine,
                             String installationName,
                             String command) {
        super(url, password, changeLogFile, username, defaultSchemaName, liquibasePropertiesPath, testRollbacks,
                contexts);
        this.installationName = installationName;
        this.command = command;
    }

    @Override
    protected boolean doPerform(AbstractBuild<?, ?> build,
                                Launcher launcher,
                                BuildListener listener,
                                Properties configProperties) throws InterruptedException, IOException {


        ArgumentListBuilder cliCommand = new ArgumentListBuilder();
        String execName;
        if (launcher.isUnix()) {
            execName = LiquibaseInstallation.UNIX_EXEC_NAME;
        } else {
            execName = LiquibaseInstallation.WINDOWS_EXEC_NAME;
        }

        cliCommand.add(new File(getInstallation().getHome(), execName));
        cliCommand.add("--" + LiquibaseProperty.LOG_LEVEL.getOption(), DEFAULT_LOG_LEVEL);
        addOptionIfPresent(cliCommand, LiquibaseProperty.DEFAULTS_FILE, getLiquibasePropertiesPath());

        cliCommand.add("update");
        Annotator annotator = new Annotator(listener.getLogger(), build.getCharset());
        try {
            launcher.launch().cmds(cliCommand).stderr(annotator).stdout(annotator).pwd(build.getWorkspace()).join();
        } catch (IOException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.FAILURE);
        } catch (InterruptedException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.FAILURE);
        }

        return true;
    }

    public LiquibaseInstallation getInstallation() {
        LiquibaseInstallation found = null;
        if (installationName != null) {
            for (LiquibaseInstallation i : DESCRIPTOR.getInstallations()) {
                if (installationName.equals(i.getName())) {
                    found = i;
                    break;
                }
            }
        }
        return found;
    }

    public static void addOptionIfPresent(ArgumentListBuilder cmdExecArgs,
                                          LiquibaseProperty liquibaseProperty,
                                          String value) {
        if (!Strings.isNullOrEmpty(value)) {
            cmdExecArgs.add("--" + liquibaseProperty.getOption(), value);
        }
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private LiquibaseInstallation[] installations;

        public DescriptorImpl() {
            load();
        }

        public DescriptorImpl(Class<? extends LiquibaseExecutor> clazz) {
            super(clazz);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Execute Liquibase";
        }

        public LiquibaseInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(LiquibaseInstallation.DescriptorImpl.class);
        }

        public LiquibaseInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(LiquibaseInstallation... installations) {
            this.installations = installations;
            save();
        }
    }
}
