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

/**
 * Jenkins builder which runs liquibase.
 */
public class LiquibaseBuilder extends Builder {

    private static final String DEFAULT_LOGLEVEL = "info";
    private static final String OPTION_HYPHENS = "--";

    /**
     * The liquibase action to execute.
     */
    private String liquibaseCommand;
    /**
     * Which liquibase installation to use during invocation.
     */
    private String installationName;

    /**
     * Root changeset file.
     */
    private String changeLogFile;
    /**
     * Username with which to connect to database.
     */
    private String username;
    /**
     * Password with which to connect to database.
     */
    private String password;
    /**
     * JDBC database connection URL.
     */
    private String url;
    private String defaultSchemaName;
    /**
     * Contexts to activate during execution.
     */
    private String contexts;
    /**
     * File in which configuration options may be found.
     */
    private String defaultsFile;
    /**
     * Class name of database driver.
     */
    private String driverClassName;

    /**
     * Catch-all option which can be used to supply additional options to liquibase.
     */
    private String commandLineArgs;

    @Extension
    public static final LiquibaseStepDescriptor DESCRIPTOR = new LiquibaseStepDescriptor();

    @DataBoundConstructor
    public LiquibaseBuilder(String commandLineArgs,
                            String changeLogFile,
                            String liquibaseCommand,
                            String installationName,
                            String username,
                            String password,
                            String url,
                            String defaultSchemaName,
                            String contexts,
                            String defaultsFile,
                            String driverClassName) {
        this.changeLogFile = changeLogFile;

        this.liquibaseCommand = liquibaseCommand;
        this.installationName = installationName;
        this.username = username;
        this.password = password;
        this.url = url;
        this.defaultSchemaName = defaultSchemaName;
        this.contexts = contexts;
        this.defaultsFile = defaultsFile;
        this.driverClassName = driverClassName;

        this.commandLineArgs = commandLineArgs;
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

        ArgumentListBuilder cliCommand = new ArgumentListBuilder();
        cliCommand.add(new File(getInstallation().getHome()));

        addOptionIfPresent(cliCommand, CliOption.CHANGELOG_FILE, changeLogFile);
        addOptionIfPresent(cliCommand, CliOption.USERNAME, username);
        if (!Strings.isNullOrEmpty(password)) {
            cliCommand.add(OPTION_HYPHENS + CliOption.PASSWORD.getCliOption());
            cliCommand.addMasked(password);
        }
        addOptionIfPresent(cliCommand, CliOption.DEFAULTS_FILE, defaultsFile);
        addOptionIfPresent(cliCommand, CliOption.CONTEXTS, contexts);
        addOptionIfPresent(cliCommand, CliOption.URL, url);
        addOptionIfPresent(cliCommand, CliOption.DEFAULT_SCHEMA_NAME, defaultSchemaName);
        addOptionIfPresent(cliCommand, CliOption.DATABASE_DRIVER_NAME, driverClassName);

        if (!Strings.isNullOrEmpty(commandLineArgs)) {
            cliCommand.addTokenized(commandLineArgs);
        }
        cliCommand.add(OPTION_HYPHENS + CliOption.LOG_LEVEL.getCliOption(), DEFAULT_LOGLEVEL);

        cliCommand.addTokenized(liquibaseCommand);

        int exitStatus = launcher.launch().cmds(cliCommand).stdout(listener).pwd(build.getWorkspace()).join();

        boolean result = true;
        if (exitStatus != 0) {
            result = false;
        } else {
            // check for errors that don't result
            File logFile = build.getLogFile();
            if (Util.doesErrorExist(logFile)) {
                result = false;
            }
        }
        return result;
    }

    private static void addOptionIfPresent(ArgumentListBuilder cmdExecArgs, CliOption cliOption, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            cmdExecArgs.add(OPTION_HYPHENS + cliOption.getCliOption(), value);
        }
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

    public String getContexts() {
        return contexts;
    }

    public String getDefaultsFile() {
        return defaultsFile;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url;
    }

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }
}
