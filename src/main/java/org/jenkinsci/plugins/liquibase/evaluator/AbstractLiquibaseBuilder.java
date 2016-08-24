package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Builder;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.ResourceAccessor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jenkinsci.plugins.liquibase.common.LiquibaseProperty;
import org.jenkinsci.plugins.liquibase.common.PropertiesAssembler;
import org.jenkinsci.plugins.liquibase.common.Util;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public abstract class AbstractLiquibaseBuilder extends Builder {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLiquibaseBuilder.class);

    protected String databaseEngine;
    protected String changeLogFile;
    protected String username;
    protected String password;
    protected String url;
    protected String defaultSchemaName;
    protected String contexts;
    protected String liquibasePropertiesPath;
    protected String classpath;
    protected String driverClassname;
    protected String labels;
    private String changeLogParameters;


    public AbstractLiquibaseBuilder(String databaseEngine,
                                    String changeLogFile,
                                    String username,
                                    String password,
                                    String url,
                                    String defaultSchemaName,
                                    String contexts,
                                    String liquibasePropertiesPath,
                                    String classpath,
                                    String driverClassname,
                                    String changeLogParameters, String labels) {
        this.databaseEngine = databaseEngine;
        this.changeLogFile = changeLogFile;
        this.username = username;
        this.password = password;
        this.url = url;
        this.defaultSchemaName = defaultSchemaName;
        this.contexts = contexts;
        this.liquibasePropertiesPath = liquibasePropertiesPath;
        this.classpath = classpath;
        this.driverClassname = driverClassname;
        this.changeLogParameters = changeLogParameters;
        this.labels = labels;
    }

    public AbstractLiquibaseBuilder() {

    }

    public abstract void doPerform(AbstractBuild<?, ?> build,
                                   BuildListener listener,
                                   Liquibase liquibase,
                                   Contexts contexts,
                                   ExecutedChangesetAction executedChangesetAction)
    throws InterruptedException, IOException, LiquibaseException;

    abstract public Descriptor<Builder> getDescriptor();

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Properties configProperties = PropertiesAssembler.createLiquibaseProperties(this, build);
        ExecutedChangesetAction executedChangesetAction = new ExecutedChangesetAction(build);
        Liquibase liquibase = createLiquibase(build, listener, executedChangesetAction, configProperties, launcher);
        String liqContexts = getProperty(configProperties, LiquibaseProperty.CONTEXTS);
        Contexts contexts = new Contexts(liqContexts);
        try {
            doPerform(build, listener, liquibase, contexts, executedChangesetAction);
        } catch (LiquibaseException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.UNSTABLE);
        } finally {
            closeLiquibase(liquibase);
        }
        if (!executedChangesetAction.isRollbackOnly()) {
            build.addAction(executedChangesetAction);
        }
        return true;
    }

    public Liquibase createLiquibase(AbstractBuild<?, ?> build,
                                     BuildListener listener,
                                     ExecutedChangesetAction action,
                                     Properties configProperties,
                                     Launcher launcher) {
        Liquibase liquibase;
        String driverName = getProperty(configProperties, LiquibaseProperty.DRIVER);

        try {
            if (!Strings.isNullOrEmpty(classpath)) {
                Util.addClassloader(launcher.isUnix(), build.getWorkspace(), classpath);
            }

            JdbcConnection jdbcConnection = createJdbcConnection(configProperties, driverName);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

            ResourceAccessor filePathAccessor = new FilePathAccessor(build);
            CompositeResourceAccessor resourceAccessor =
                    new CompositeResourceAccessor(filePathAccessor,
                            new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()),
                            new ClassLoaderResourceAccessor(ClassLoader.getSystemClassLoader())
                    );


            String changeLogFile = getProperty(configProperties, LiquibaseProperty.CHANGELOG_FILE);
            liquibase = new Liquibase(changeLogFile, resourceAccessor, database);

        } catch (LiquibaseException e) {
            throw new RuntimeException("Error creating liquibase database.", e);
        }
        BuildChangeExecListener buildChangeExecListener = new BuildChangeExecListener(action, listener);
        liquibase.setChangeExecListener(buildChangeExecListener);
        if (!Strings.isNullOrEmpty(changeLogParameters)) {
            Map<String, String> keyValuePairs = Splitter.on("\n").withKeyValueSeparator("=").split(changeLogParameters);
            for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
                liquibase.setChangeLogParameter(entry.getKey(), entry.getValue());
            }
        }
        return liquibase;
    }

    private JdbcConnection createJdbcConnection(Properties configProperties, String driverName) {
        Connection connection;
        String dbUrl = getProperty(configProperties, LiquibaseProperty.URL);
        try {
            Util.registerDatabaseDriver(driverName, classpath);
            String userName = getProperty(configProperties, LiquibaseProperty.USERNAME);
            String password = getProperty(configProperties, LiquibaseProperty.PASSWORD);
            connection = DriverManager.getConnection(dbUrl, userName, password);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error getting database connection using driver " + driverName + " using url '" + dbUrl + "'", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Error registering database driver " + driverName, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error registering database driver " + driverName, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error registering database driver " + driverName, e);
        }
        return new JdbcConnection(connection);
    }

    protected static String getProperty(Properties configProperties, LiquibaseProperty property) {
        return configProperties.getProperty(property.propertyName());
    }

    private static void closeLiquibase(Liquibase liquibase) {
        if (liquibase.getDatabase() != null) {
            try {
                DatabaseConnection connection = liquibase.getDatabase().getConnection();
                if (!connection.isClosed()) {
                    try {
                        connection.close();
                    } catch (DatabaseException e) {
                        LOG.warn("error closing connection",e);
                    }
                }
            } catch (DatabaseException e) {
                LOG.warn("error closing database", e);
            }
        }
    }

    public List<IncludedDatabaseDriver> getDrivers() {
        return ChangesetEvaluator.DESCRIPTOR.getIncludedDatabaseDrivers();
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

    public String getLiquibasePropertiesPath() {
        return liquibasePropertiesPath;
    }

    @DataBoundSetter
    public void setLiquibasePropertiesPath(String liquibasePropertiesPath) {
        this.liquibasePropertiesPath = liquibasePropertiesPath;
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

    public String getChangeLogParameters() {
        return changeLogParameters;
    }

    @DataBoundSetter
    public void setChangeLogParameters(String changeLogParameters) {
        this.changeLogParameters = changeLogParameters;
    }

    public String getLabels() {
        return labels;
    }

    @DataBoundSetter
    public void setLabels(String labels) {
        this.labels = labels;
    }
}
