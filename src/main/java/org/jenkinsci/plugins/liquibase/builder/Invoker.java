package org.jenkinsci.plugins.liquibase.builder;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.util.ArgumentListBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.jenkinsci.plugins.liquibase.installation.LiquibaseInstallation;

import com.google.common.base.Strings;

public class Invoker {

    private static final String DEFAULT_LOG_LEVEL = "info";

    public boolean invokeLiquibase(Properties properties,
                                   BuildListener listener,
                                   Launcher launcher,
                                   AbstractBuild<?, ?> build,
                                   LiquibaseBuilder liquibaseBuilder,
                                   Annotator annotator) {
        ArgumentListBuilder cliCommand = new ArgumentListBuilder();
        String execName;
        if (launcher.isUnix()) {
            execName = LiquibaseInstallation.UNIX_EXEC_NAME;
        } else {
            execName = LiquibaseInstallation.WINDOWS_EXEC_NAME;
        }


        cliCommand.add(new File(liquibaseBuilder.getInstallation().getHome(), execName));
        cliCommand.add("--" + LiquibaseProperty.LOG_LEVEL.getOption(), DEFAULT_LOG_LEVEL);
        addOptionIfPresent(cliCommand, LiquibaseProperty.DEFAULTS_FILE, liquibaseBuilder.getLiquibasePropertiesPath());

        cliCommand.add("update");

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

    private static void addOptionIfPresent(ArgumentListBuilder cmdExecArgs, LiquibaseProperty liquibaseProperty, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            cmdExecArgs.add("--" + liquibaseProperty.getOption(), value);
        }
    }
}
