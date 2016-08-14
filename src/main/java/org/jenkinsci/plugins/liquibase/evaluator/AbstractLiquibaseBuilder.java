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
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
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

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public abstract class AbstractLiquibaseBuilder extends Builder {
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
    protected static String getProperty(Properties configProperties, LiquibaseProperty property) {
        return configProperties.getProperty(property.propertyName());
    }


    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        RolledbackChangesetAction rolledbackChangesetAction = new RolledbackChangesetAction(build);

        Properties configProperties = PropertiesAssembler.createLiquibaseProperties(this, build);
        ExecutedChangesetAction executedChangesetAction = new ExecutedChangesetAction(build);
        Liquibase liquibase = createLiquibase(build, listener, executedChangesetAction, configProperties, launcher, rolledbackChangesetAction);
        String liqContexts = getProperty(configProperties, LiquibaseProperty.CONTEXTS);
        Contexts contexts = new Contexts(liqContexts);
        try {
            doPerform(build, listener, liquibase, contexts, rolledbackChangesetAction, executedChangesetAction);
        } catch (LiquibaseException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.UNSTABLE);
        }

        if (executedChangesetAction.isShouldSummarize()) {
            build.addAction(executedChangesetAction);
        }

        if (rolledbackChangesetAction.isShouldSummarize()) {
            build.addAction(rolledbackChangesetAction);
        }


        return true;
    };

    public abstract void doPerform(AbstractBuild<?, ?> build,
                                   BuildListener listener,
                                   Liquibase liquibase,
                                   Contexts contexts,
                                   RolledbackChangesetAction rolledbackChangesetAction,
                                   ExecutedChangesetAction executedChangesetAction)
            throws InterruptedException, IOException, LiquibaseException;

    public Liquibase createLiquibase(AbstractBuild<?, ?> build,
                                     BuildListener listener,
                                     ExecutedChangesetAction action,
                                     Properties configProperties,
                                     Launcher launcher,
                                     RolledbackChangesetAction rolledbackChangesetAction) {
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
        liquibase.setChangeExecListener(new BuildChangeExecListener(action,rolledbackChangesetAction, listener));
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

    public List<IncludedDatabaseDriver> getDrivers() {
        return ChangesetEvaluator.DESCRIPTOR.getIncludedDatabaseDrivers();
    }


    abstract public Descriptor<Builder> getDescriptor();


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
