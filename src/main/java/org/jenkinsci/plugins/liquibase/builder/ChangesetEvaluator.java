package org.jenkinsci.plugins.liquibase.builder;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.MigrationFailedException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.jenkinsci.plugins.liquibase.common.AbstractLiquibaseBuildStep;
import org.jenkinsci.plugins.liquibase.common.LiquibaseProperty;
import org.jenkinsci.plugins.liquibase.common.PropertiesParser;
import org.jenkinsci.plugins.liquibase.installation.LiquibaseInstallation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Jenkins builder which runs liquibase.
 */
@SuppressWarnings("ProhibitedExceptionThrown")
public class ChangesetEvaluator extends AbstractLiquibaseBuildStep {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    protected static final String DEFAULT_LOGLEVEL = "info";

    protected static final String OPTION_HYPHENS = "--";
    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluator.class);

    protected String databaseEngine;
    private boolean invokeExternal;
    private boolean dropAll;



    @DataBoundConstructor
    public ChangesetEvaluator(String url,
                              String password,
                              String changeLogFile,
                              String username,
                              String defaultSchemaName,
                              String liquibasePropertiesPath,
                              boolean testRollbacks,
                              String contexts,
                              String databaseEngine,
                              String installationName,
                              boolean dropAll) {
        super(url, password, changeLogFile, username, defaultSchemaName, liquibasePropertiesPath, testRollbacks,
                contexts);
        this.databaseEngine = databaseEngine;
        this.dropAll = dropAll;
    }

    private ChangesetEvaluator(ChangesetEvaluatorBuilder changesetEvaluatorBuilder) {
        databaseEngine = changesetEvaluatorBuilder.databaseEngine;
        setInvokeExternal(changesetEvaluatorBuilder.invokeExternal);
        setDropAll(changesetEvaluatorBuilder.dropAll);
        setChangeLogFile(changesetEvaluatorBuilder.changeLogFile);
        setUsername(changesetEvaluatorBuilder.username);
        setPassword(changesetEvaluatorBuilder.password);
        setUrl(changesetEvaluatorBuilder.url);
        setDefaultSchemaName(changesetEvaluatorBuilder.defaultSchemaName);
        setContexts(changesetEvaluatorBuilder.contexts);
        setTestRollbacks(changesetEvaluatorBuilder.testRollbacks);
        setLiquibasePropertiesPath(changesetEvaluatorBuilder.liquibasePropertiesPath);
    }


    @Override
    public boolean doPerform(final AbstractBuild<?, ?> build,
                             Launcher launcher,
                             BuildListener listener,
                             Properties configProperties)
            throws InterruptedException, IOException {

        PropertiesParser.setDriverFromDBEngine(this, configProperties);
        ExecutedChangesetAction action = new ExecutedChangesetAction(build);
        Liquibase liquibase = createLiquibase(build, listener, action, configProperties);
        String liqContexts = getProperty(configProperties, LiquibaseProperty.CONTEXTS);

        try {
            if (dropAll) {
                liquibase.dropAll();
            }
            if (testRollbacks) {
                liquibase.updateTestingRollback(liqContexts);
            } else {
                liquibase.update(new Contexts(liqContexts));
            }

        } catch (MigrationFailedException migrationException) {
            migrationException.printStackTrace(listener.getLogger());
            build.setResult(Result.UNSTABLE);
        } catch (LiquibaseException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.FAILURE);
        } finally {
            if (liquibase.getDatabase() != null) {
                try {
                    liquibase.getDatabase().close();
                } catch (DatabaseException e) {
                    LOG.warn("error closing database", e);
                }
            }
        }
        build.addAction(action);
        return true;
    }

    public static Liquibase createLiquibase(AbstractBuild<?, ?> build,
                                            BuildListener listener,
                                            ExecutedChangesetAction action,
                                            Properties configProperties) {
        Liquibase liquibase;

        LiquibaseProperty property = LiquibaseProperty.DRIVER;
        String driverName = getProperty(configProperties, property);
        try {
            Connection connection = getDatabaseConnection(configProperties, driverName);
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database =
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

            liquibase = new Liquibase(configProperties.getProperty(LiquibaseProperty.CHANGELOG_FILE.getOption()),
                    new FilePathAccessor(build), database);


        } catch (LiquibaseException e) {
            throw new RuntimeException("Error creating liquibase database.", e);
        }
        liquibase.setChangeExecListener(new BuildChangeExecListener(action, listener));
        return liquibase;
    }

    protected static Connection getDatabaseConnection(Properties configProperties, String driverName) {
        Connection connection;
        String dbUrl = getProperty(configProperties, LiquibaseProperty.URL);
        try {
            DriverManager.registerDriver((Driver) Class.forName(driverName).newInstance());
            connection = DriverManager
                    .getConnection(dbUrl, getProperty(configProperties, LiquibaseProperty.USERNAME),
                            getProperty(configProperties,
                                    LiquibaseProperty.PASSWORD));
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error getting database connection using driver " + driverName + " using url '" + dbUrl + "'", e);
        } catch (InstantiationException e) {
            throw new RuntimeException(
                    "Error registering database driver " + driverName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(
                    "Error registering database driver " + driverName, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Error registering database driver " + driverName, e);
        }
        return connection;
    }

    protected static String getProperty(Properties configProperties, LiquibaseProperty property) {
        return configProperties.getProperty(property.getOption());
    }

    public List<EmbeddedDriver> getDrivers() {
        return DESCRIPTOR.getEmbeddedDrivers();
    }

    /**
     * Reflection necessary because failedChangeset has private access on {@link MigrationFailedException}.
     *
     * @param me
     * @return
     */
    private static Optional<ChangeSet> reflectFailed(MigrationFailedException me) {
        ChangeSet failed = null;
        try {
            Field field = me.getClass().getDeclaredField("failedChangeSet");
            field.setAccessible(true);
            failed = (ChangeSet) field.get(me);
            if (LOG.isDebugEnabled()) {
                LOG.debug("retrieved reference to failed changeset[" + failed + "] ");
            }

        } catch (NoSuchFieldException ignored) {
        } catch (IllegalAccessException ignored) {
        }
        return Optional.fromNullable(failed);
    }

    protected static void addOptionIfPresent(ArgumentListBuilder cmdExecArgs,
                                             LiquibaseProperty liquibaseProperty,
                                             String value) {
        if (!Strings.isNullOrEmpty(value)) {
            cmdExecArgs.add(OPTION_HYPHENS + liquibaseProperty.getOption(), value);
        }
    }

    @Override
    public Descriptor<Builder > getDescriptor() {
        return DESCRIPTOR;
    }

    public boolean isTestRollbacks() {
        return testRollbacks;
    }

    public void setInvokeExternal(boolean invokeExternal) {
        this.invokeExternal = invokeExternal;
    }

    public boolean isInvokeExternal() {
        return invokeExternal;
    }

    public boolean isDropAll() {
        return dropAll;
    }

    public void setDropAll(boolean dropAll) {
        this.dropAll = dropAll;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    public void setTestRollbacks(boolean testRollbacks) {
        this.testRollbacks = testRollbacks;
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private List<EmbeddedDriver> embeddedDrivers;
        private LiquibaseInstallation[] installations;

        public DescriptorImpl() {
            load();
        }

        public DescriptorImpl(Class<? extends ChangesetEvaluator> clazz) {
            super(clazz);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Evaluate liquibase changesets";
        }

        public List<EmbeddedDriver> getEmbeddedDrivers() {
            if (embeddedDrivers == null) {
                initDriverList();
            }
            return embeddedDrivers;
        }

        public LiquibaseInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(LiquibaseInstallation.DescriptorImpl.class);
        }

        private void initDriverList() {
            embeddedDrivers = Lists.newArrayList(new EmbeddedDriver("MySQL", "com.mysql.jdbc.Driver"),
                    new EmbeddedDriver("PostgreSQL", "org.postgresql.Driver"),
                    new EmbeddedDriver("Derby", "org.apache.derby.jdbc.EmbeddedDriver"),
                    new EmbeddedDriver("Hypersonic", "org.hsqldb.jdbcDriver"),
                    new EmbeddedDriver("H2", "org.h2.Driver"));
        }

        public LiquibaseInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(LiquibaseInstallation... installations) {
            this.installations = installations;
            save();
        }
    }

    public static final class ChangesetEvaluatorBuilder {
        private String databaseEngine;
        private boolean invokeExternal;
        private boolean dropAll;
        private String changeLogFile;
        private String username;
        private String password;
        private String url;
        private String defaultSchemaName;
        private String contexts;
        private boolean testRollbacks;
        private String liquibasePropertiesPath;

        public ChangesetEvaluatorBuilder() {
        }

        public ChangesetEvaluatorBuilder withDatabaseEngine(String val) {
            databaseEngine = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withInvokeExternal(boolean val) {
            invokeExternal = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withDropAll(boolean val) {
            dropAll = val;
            return this;
        }


        public ChangesetEvaluatorBuilder withChangeLogFile(File val) {
            changeLogFile = val.getAbsolutePath();
            return this;
        }

        public ChangesetEvaluatorBuilder withChangeLogFile(String val) {
            changeLogFile = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withUsername(String val) {
            username = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withPassword(String val) {
            password = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withUrl(String val) {
            url = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withDefaultSchemaName(String val) {
            defaultSchemaName = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withContexts(String val) {
            contexts = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withTestRollbacks(boolean val) {
            testRollbacks = val;
            return this;
        }

        public ChangesetEvaluatorBuilder withLiquibasePropertiesPath(String val) {
            liquibasePropertiesPath = val;
            return this;
        }

        public ChangesetEvaluator build() {
            return new ChangesetEvaluator(this);
        }
    }
}