package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.MigrationFailedException;
import liquibase.resource.ResourceAccessor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jenkinsci.plugins.liquibase.common.LiquibaseCommand;
import org.jenkinsci.plugins.liquibase.common.LiquibaseProperty;
import org.jenkinsci.plugins.liquibase.common.PropertiesAssembler;
import org.jenkinsci.plugins.liquibase.common.Util;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Jenkins builder which evaluates liquibase changesets.
 */
@SuppressWarnings("ProhibitedExceptionThrown")
public class ChangesetEvaluator extends Builder {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluator.class);

    protected String databaseEngine;
    protected String changeLogFile;
    protected String username;
    protected String password;
    protected String url;
    protected String defaultSchemaName;
    protected String contexts;
    protected boolean testRollbacks;
    protected String liquibasePropertiesPath;
    private boolean dropAll;
    private String classpath;
    private String driverClassname;
    private String labels;
    private String changeLogParameters;


    @DataBoundConstructor
    public ChangesetEvaluator(String databaseEngine,
                              String changeLogFile,
                              String username,
                              String password,
                              String url,
                              String defaultSchemaName,
                              String contexts,
                              boolean testRollbacks,
                              String liquibasePropertiesPath,
                              boolean dropAll,
                              String command,
                              String classpath,
                              String driverClassname, String labels) {
        this.databaseEngine = databaseEngine;
        this.changeLogFile = changeLogFile;
        this.username = username;
        this.password = password;
        this.url = url;
        this.defaultSchemaName = defaultSchemaName;
        this.contexts = contexts;
        this.testRollbacks = testRollbacks;
        this.liquibasePropertiesPath = liquibasePropertiesPath;
        this.dropAll = dropAll;
        this.classpath = classpath;
        this.driverClassname = driverClassname;
        this.labels = labels;
    }

    public ChangesetEvaluator() {

    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        Properties configProperties = PropertiesAssembler.createLiquibaseProperties(this, build);
        ExecutedChangesetAction action = new ExecutedChangesetAction(build);
        Liquibase liquibase = createLiquibase(build, listener, action, configProperties, launcher);
        String liqContexts = getProperty(configProperties, LiquibaseProperty.CONTEXTS);
        try {
            String resolvedCommand;
            if (isTestRollbacks()) {
                resolvedCommand = LiquibaseCommand.UPDATE_TESTING_ROLLBACKS.getCommand();
            } else {
                resolvedCommand = LiquibaseCommand.UPDATE.getCommand();
            }

            if (dropAll) {
                listener.getLogger().println("Running liquibase dropAll");
                liquibase.dropAll();
            }
            listener.getLogger().println("Running liquibase command '" + resolvedCommand + "'");

            if (LiquibaseCommand.UPDATE_TESTING_ROLLBACKS.isCommand(resolvedCommand)) {
                liquibase.updateTestingRollback(new Contexts(liqContexts), new liquibase.LabelExpression(labels));
            }

            if (LiquibaseCommand.UPDATE.isCommand(resolvedCommand)) {
                liquibase.update(new Contexts(liqContexts), new liquibase.LabelExpression(labels));
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

    public Liquibase createLiquibase(AbstractBuild<?, ?> build,
                                     BuildListener listener,
                                     ExecutedChangesetAction action,
                                     Properties configProperties, Launcher launcher) {
        Liquibase liquibase;
        String driverName = getProperty(configProperties, LiquibaseProperty.DRIVER);

        try {
            if (!Strings.isNullOrEmpty(classpath)) {
                Util.addClassloader(launcher.isUnix(), build.getWorkspace(), classpath);
            }

            Connection connection = retrieveConnection(configProperties, driverName);
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database =
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

            ResourceAccessor resourceAccessor = new FilePathAccessor(build);
            liquibase = new Liquibase(configProperties.getProperty(LiquibaseProperty.CHANGELOG_FILE.propertyName()),
                    resourceAccessor, database);

        } catch (LiquibaseException e) {
            throw new RuntimeException("Error creating liquibase database.", e);
        }
        liquibase.setChangeExecListener(new BuildChangeExecListener(action, listener));
        if (!Strings.isNullOrEmpty(changeLogParameters)) {
            Map<String, String> keyValuePairs = Splitter.on("\n").withKeyValueSeparator("=").split(changeLogParameters);
            for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
                liquibase.setChangeLogParameter(entry.getKey(), entry.getValue());

            }
        }
        return liquibase;
    }

    protected Connection retrieveConnection(Properties configProperties, String driverName) {
        Connection connection;
        String dbUrl = getProperty(configProperties, LiquibaseProperty.URL);
        try {
            Util.registerDatabaseDriver(driverName, classpath);
            connection = DriverManager.getConnection(dbUrl, getProperty(configProperties, LiquibaseProperty.USERNAME),
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
        return configProperties.getProperty(property.propertyName());
    }

    public List<IncludedDatabaseDriver> getDrivers() {
        return DESCRIPTOR.getIncludedDatabaseDrivers();
    }

    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    @DataBoundSetter
    public void setDatabaseEngine(String databaseEngine) {
        this.databaseEngine = databaseEngine;
    }

    public String getChangeLogFile() {
        return changeLogFile;
    }

    @DataBoundSetter
    public void setChangeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    public String getUsername() {
        return username;
    }

    @DataBoundSetter
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    @DataBoundSetter
    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    public String getDefaultSchemaName() {
        return defaultSchemaName;
    }

    @DataBoundSetter
    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    public String getContexts() {
        return contexts;
    }

    @DataBoundSetter
    public void setContexts(String contexts) {
        this.contexts = contexts;
    }

    public boolean isTestRollbacks() {
        return testRollbacks;
    }

    @DataBoundSetter
    public void setTestRollbacks(boolean testRollbacks) {
        this.testRollbacks = testRollbacks;
    }

    public String getLiquibasePropertiesPath() {
        return liquibasePropertiesPath;
    }

    @DataBoundSetter
    public void setLiquibasePropertiesPath(String liquibasePropertiesPath) {
        this.liquibasePropertiesPath = liquibasePropertiesPath;
    }

    public boolean isDropAll() {
        return dropAll;
    }

    @DataBoundSetter
    public void setDropAll(boolean dropAll) {
        this.dropAll = dropAll;
    }

    public String getClasspath() {
        return classpath;
    }

    @DataBoundSetter
    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public String getDriverClassname() {
        return driverClassname;
    }

    @DataBoundSetter
    public void setDriverClassname(String driverClassname) {
        this.driverClassname = driverClassname;
    }

    public String getLabels() {
        return labels;
    }

    @DataBoundSetter
    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getChangeLogParameters() {
        return changeLogParameters;
    }

    @DataBoundSetter
    public void setChangeLogParameters(String changeLogParameters) {
        this.changeLogParameters = changeLogParameters;
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private List<IncludedDatabaseDriver> includedDatabaseDrivers;


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

        public List<IncludedDatabaseDriver> getIncludedDatabaseDrivers() {
            if (includedDatabaseDrivers == null) {
                initDriverList();
            }
            return includedDatabaseDrivers;
        }

        private void initDriverList() {
            includedDatabaseDrivers = Lists.newArrayList(new IncludedDatabaseDriver("MySQL", "com.mysql.jdbc.Driver"),
                    new IncludedDatabaseDriver("PostgreSQL", "org.postgresql.Driver"),
                    new IncludedDatabaseDriver("Derby", "org.apache.derby.jdbc.EmbeddedDriver"),
                    new IncludedDatabaseDriver("Hypersonic", "org.hsqldb.jdbcDriver"),
                    new IncludedDatabaseDriver("H2", "org.h2.Driver"));
        }
    }

}