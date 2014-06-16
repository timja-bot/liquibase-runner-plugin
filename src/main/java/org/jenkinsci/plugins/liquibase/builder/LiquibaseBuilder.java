package org.jenkinsci.plugins.liquibase.builder;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;

import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Jenkins builder which runs liquibase.
 */
@SuppressWarnings("ProhibitedExceptionThrown")
public class LiquibaseBuilder extends Builder {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    protected static final String DEFAULT_LOGLEVEL = "info";
    protected static final String OPTION_HYPHENS = "--";
    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseBuilder.class);

    protected List<EmbeddedDriver> embeddedDrivers =
            Lists.newArrayList(new EmbeddedDriver("MySQL", "com.mysql.jdbc.Driver"),
                    new EmbeddedDriver("PostgreSQL", "org.postgresql.Driver"),
                    new EmbeddedDriver("Hypersonic SQL", "org.hsqldb.jdbc.JDBCDriver"),
                    new EmbeddedDriver("H2", "org.h2.Driver"));
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

    protected boolean testRollbacks;

    private String databaseEngine;

    @DataBoundConstructor
    public LiquibaseBuilder(String changeLogFile, String username,
                            String password,
                            String url,
                            String defaultSchemaName,
                            String contexts,
                            String databaseEngine,
                            boolean testRollbacks) {
        this.password = password;
        this.defaultSchemaName = defaultSchemaName;
        this.url = url;
        this.username = username;

        this.changeLogFile = changeLogFile;
        this.contexts = contexts;

        this.databaseEngine = databaseEngine;
        this.testRollbacks=testRollbacks;


    }

    protected static void addOptionIfPresent(ArgumentListBuilder cmdExecArgs, CliOption cliOption, String value) {
        if (!Strings.isNullOrEmpty(value)) {
            cmdExecArgs.add(OPTION_HYPHENS + cliOption.getCliOption(), value);
        }
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Database databaseObject = null;
        try {
            String driver = getDriverName();
            databaseObject = CommandLineUtils
                    .createDatabaseObject(getClass().getClassLoader(), this.url, this.username, this.password, driver,
                            null, null, true, true, null, null, null, null);

            Liquibase liquibase = new Liquibase(changeLogFile, new FilePathAccessor(build), databaseObject);
            ExecutedChangesetAction action = new ExecutedChangesetAction(build);
            liquibase.setChangeExecListener(new BuildChangeExecListener(action, listener));

            if (testRollbacks) {
                liquibase.updateTestingRollback(contexts);
            } else {
                liquibase.update(contexts);
            }
            build.addAction(action);
        } catch (DatabaseException e) {
            e.printStackTrace(listener.getLogger());
            throw new RuntimeException("Error creating liquibase database", e);
        } catch (LiquibaseException e) {
            e.printStackTrace(listener.getLogger());
            throw new RuntimeException("Error executing liquibase liquibase database", e);
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

    private String getDriverName() {
        String driver = null;

        for (EmbeddedDriver embeddedDriver : embeddedDrivers) {
            if (embeddedDriver.getDisplayName().equals(databaseEngine)) {
                driver = embeddedDriver.getDriverClassName();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("using db driver class[" + driver + "] ");
                }
                break;
            }
        }
        return driver;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }

    public boolean isTestRollbacks() {
        return testRollbacks;
    }

    public String getContexts() {
        return contexts;
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

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private List<EmbeddedDriver> embeddedDrivers;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Invoke Liquibase";
        }

        public List<EmbeddedDriver> getEmbeddedDrivers() {
            if (embeddedDrivers == null) {
                initDriverList();
            }
            return embeddedDrivers;
        }

        private void initDriverList() {
            embeddedDrivers = Lists.newArrayList(new EmbeddedDriver("MySQL", "com.mysql.jdbc.Driver"),
                    new EmbeddedDriver("PostgreSQL", "org.postgresql.Driver"),
                    new EmbeddedDriver("Hypersonic", "org.hsqldb.jdbcDriver"),
                    new EmbeddedDriver("H2", "org.h2.Driver"));
        }
    }

}