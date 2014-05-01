package org.jenkinsci.plugins.liquibase.builder;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ResourceAccessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.jenkinsci.plugins.liquibase.installation.LiquibaseInstallation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Jenkins builder which runs liquibase.
 */
public class LiquibaseBuilder extends Builder {

    @Extension
    public static final LiquibaseStepDescriptor DESCRIPTOR = new LiquibaseStepDescriptor();
    protected static final String DEFAULT_LOGLEVEL = "info";
    protected static final String OPTION_HYPHENS = "--";
    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseBuilder.class);
    /**
     * The liquibase action to execute.
     */
    protected String liquibaseCommand;
    /**
     * Which liquibase installation to use during invocation.
     */
    protected String installationName;
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
                            String liquibaseCommand,
                            String installationName,
                            String username,
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
        this.installationName = installationName;
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
                            this.driverClassName, null, null, true, true, null,
                            null, null, null);

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
        }  finally {
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

    public boolean performd(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Annotator annotator = new Annotator(listener.getLogger(), build.getCharset());

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

        int exitStatus = launcher.launch().cmds(cliCommand).stderr(annotator).stdout(annotator).pwd(build.getWorkspace()).join();

        boolean result = true;
        if (exitStatus != 0) {
            result = false;
        } else {
            // check for errors that don't result in an exit code less than 0.
            File logFile = build.getLogFile();
            if (Util.doesErrorExist(logFile)) {
                result = false;
            }
        }
        return result;
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

    private static class FilePathAccessor implements ResourceAccessor {
        private final AbstractBuild<?, ?> build;

        public FilePathAccessor(AbstractBuild<?, ?> build) {
            this.build = build;
        }

        public InputStream getResourceAsStream(String s) throws IOException {
            FilePath child = build.getWorkspace().child(s);
            InputStream inputStream=null;
            try {
                if (child.exists()) {
                    inputStream = child.read();
                }
            } catch (InterruptedException e) {
                throw new IOException("Error reading resource[" + s + "] ", e);
            }

            return inputStream;
        }

        public Enumeration<URL> getResources(String s) throws IOException {
            Enumeration<URL> o = null;
            FilePath childDir = build.getWorkspace().child(s);
            try {
                List<URL> urls= new ArrayList<URL>();
                if (childDir.isDirectory()) {
                    List<FilePath> children = childDir.list();
                    for (FilePath child : children) {
                        urls.add(child.toURI().toURL());

                    }
                    o= Collections.enumeration(urls);

                } else {
                    urls.add(childDir.toURI().toURL());

                }
            } catch (InterruptedException e) {
                throw new IOException("Error loading resources from[" + s + "] ", e);
            }


            return o;
        }

        public ClassLoader toClassLoader() {
            try {
                return new URLClassLoader(new URL[]{new URL("file://" + build.getWorkspace().getBaseName())});
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
