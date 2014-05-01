package org.jenkinsci.plugins.liquibase.builder;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Jenkins builder which runs liquibase.
 */
public class LiquibaseBuilder extends Builder {

    protected static final String DEFAULT_LOGLEVEL = "info";
    protected static final String OPTION_HYPHENS = "--";
    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseBuilder.class);
    /**
     * The liquibase action to execute.
     */
    protected String liquibaseCommand;
    /**
     * Root changeset file.
     */
    protected String changeLogFile;
    /**
     * Username with which to connect to database.
     */
    protected String username;
    /**
     * Password with which to connect to database.
     */
    protected String password;
    /**
     * JDBC database connection URL.
     */
    protected String url;
    protected String defaultSchemaName;
    /**
     * Contexts to activate during execution.
     */
    protected String contexts;
    /**
     * File in which configuration options may be found.
     */
    protected String defaultsFile;
    /**
     * Class name of database driver.
     */
    protected String driverClassName;
    /**
     * Catch-all option which can be used to supply additional options to liquibase.
     */
    protected String commandLineArgs;

    @DataBoundConstructor
    public LiquibaseBuilder(String commandLineArgs,
                            String changeLogFile,
                            String liquibaseCommand, String username,
                            String password,
                            String url,
                            String defaultSchemaName,
                            String contexts,
                            String defaultsFile,
                            String driverClassName) {
        this.password = password;
        this.defaultSchemaName = defaultSchemaName;
        this.url = url;
        this.driverClassName = driverClassName;
        this.username = username;

        this.defaultsFile = defaultsFile;
        this.changeLogFile = changeLogFile;
        this.liquibaseCommand = liquibaseCommand;
        this.commandLineArgs = commandLineArgs;
        this.contexts = contexts;

    }

    protected static void addOptionIfPresent(ArgumentListBuilder cmdExecArgs, CliOption cliOption, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            cmdExecArgs.add(OPTION_HYPHENS + cliOption.getCliOption(), value);
        }
    }

    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Database databaseObject = null;
        try {
            databaseObject = CommandLineUtils
                    .createDatabaseObject(getClass().getClassLoader(), this.url, this.username, this.password,
                            this.driverClassName, null, null, true, true, null, null, null, null);

            Liquibase liquibase = new Liquibase(changeLogFile, new FilePathAccessor(build), databaseObject);
            List<ChangeSet> changeSets = liquibase.listUnrunChangeSets(contexts);
            for (ChangeSet changeSet : changeSets) {
                LOG.info(changeSet.getId() + " has not been run. author:" + changeSet.getAuthor());
            }

            StringWriter output = new StringWriter();
            liquibase.update(contexts, output);

            LOG.info(output.toString());


        } catch (DatabaseException e) {
            throw new RuntimeException("Error creating liquibase database", e);
        } catch (LiquibaseException e) {
            throw new RuntimeException("Error creating liquibase database", e);
        } finally {
            if (databaseObject != null) {
                try {
                    databaseObject.close();
                } catch (DatabaseException e) {
                    LOG.warn("error closing database", e);
                }
            }

        }

        return true;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return new DescriptorImpl();
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

    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Invoke Liquibase";
        }
    }

}