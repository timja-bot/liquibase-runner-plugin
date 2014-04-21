package org.jenkinsci.plugins.liquibase.builder;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.plugins.liquibase.installation.LiquibaseInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Strings;

public class LiquibaseBuilder extends Builder {

    private String commandLineArgs;
    private String changeLogFile;

    private String liquibaseCommand;

    private String installationName;


    @Extension
    public static final LiquibaseStepDescriptor DESCRIPTOR = new LiquibaseStepDescriptor();

    @DataBoundConstructor
    public LiquibaseBuilder(String commandLineArgs, String changeLogFile, String liquibaseCommand, String installationName) {
        this.commandLineArgs = commandLineArgs;
        this.changeLogFile = changeLogFile;


        this.liquibaseCommand = liquibaseCommand;
        this.installationName = installationName;
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

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        boolean result = true;


        ArgumentListBuilder cmdExecArgs = new ArgumentListBuilder();
        cmdExecArgs.add(new File(getInstallation().getHome()));

        cmdExecArgs.add(commandLineArgs);

        if (!Strings.isNullOrEmpty(changeLogFile)) {
            cmdExecArgs.add("--changeLogFile", changeLogFile);
        }
        cmdExecArgs.add(liquibaseCommand);

        listener.getLogger().println("Executing : " + cmdExecArgs.toStringWithQuote());

        int exitStatus = launcher.launch().cmds(cmdExecArgs).stdout(listener).pwd(build.getWorkspace()).join();

        if (exitStatus != 0) {
            result = false;
        }


        return result;
    }

    public String getCommandLineArgs() {
        return commandLineArgs;
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }

    public String getLiquibaseCommand() {
        return liquibaseCommand;
    }

    public String getInstallationName() {
        return installationName;
    }
}
