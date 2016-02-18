package org.jenkinsci.plugins.liquibase.external;

import hudson.Extension;
import hudson.FilePath;
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
import org.jenkinsci.plugins.liquibase.common.Util;
import org.jenkinsci.plugins.liquibase.installation.LiquibaseInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Strings;

public class LiquibaseExecutor extends AbstractLiquibaseBuildStep {
    public static final String DEFAULT_LOG_LEVEL = "info";
    private String driverName;

    private String installationName;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String logLevel;

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
                             String command,
                             String logLevel,
                             String driverName

    ) {
        super(url, password, changeLogFile, username, defaultSchemaName, liquibasePropertiesPath, testRollbacks,
                contexts);
        this.driverName = driverName;
        this.installationName = installationName;
        this.command = command;
        this.logLevel = logLevel;
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
        addOptionIfPresent(cliCommand, LiquibaseProperty.DEFAULTS_FILE, liquibasePropertiesPath);
        addOptionIfPresent(cliCommand, LiquibaseProperty.CONTEXTS, contexts);
        addOptionIfPresent(cliCommand, LiquibaseProperty.DEFAULT_SCHEMA_NAME, defaultSchemaName);
        addOptionIfPresent(cliCommand, LiquibaseProperty.USERNAME, username);
        addOptionIfPresent(cliCommand, LiquibaseProperty.PASSWORD, password);
        addOptionIfPresent(cliCommand, LiquibaseProperty.DRIVER, driverName);
        addOptionIfPresent(cliCommand, LiquibaseProperty.URL, url);
        addOptionIfPresent(cliCommand, LiquibaseProperty.CHANGELOG_FILE, changeLogFile);
        String useLogLevel;
        if (Strings.isNullOrEmpty(logLevel)) {
            useLogLevel = DEFAULT_LOG_LEVEL;
        } else {
            useLogLevel = logLevel;
        }
        addOptionIfPresent(cliCommand, LiquibaseProperty.LOG_LEVEL, useLogLevel);
        cliCommand.add(command);
        Annotator annotator = new Annotator(listener.getLogger(), build.getCharset());
        try {
            FilePath workspace = build.getWorkspace();
            FilePath workingDirectory =
                    new FilePath(workspace, configProperties.getProperty(LiquibaseProperty.CHANGELOG_FILE.getOption()))
                            .getParent();
            int executionStatus =
                    launcher.launch().cmds(cliCommand).stderr(annotator).stdout(annotator).pwd(workingDirectory).join();
            boolean executionResultedInError = didExecutionResultInError(executionStatus);

            if (executionResultedInError) {
                build.setResult(Result.FAILURE);
            } else {
                if (buildLogContainsErrorMessages(build)) {
                    build.setResult(Result.UNSTABLE);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.FAILURE);
        } catch (InterruptedException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.FAILURE);
        }
        return true;
    }

    private boolean buildLogContainsErrorMessages(AbstractBuild<?, ?> build) throws IOException {
        boolean errorsArePresent = false;
        // check for errors that don't result in an exit code less than 0.
        File logFile = build.getLogFile();
        if (Util.doesErrorExist(logFile)) {
            errorsArePresent = true;
        }
        return errorsArePresent;
    }

    private boolean didExecutionResultInError(int executionStatus) {
        return executionStatus != 0;
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

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getInstallationName() {
        return installationName;
    }

    public void setInstallationName(String installationName) {
        this.installationName = installationName;
    }

}
