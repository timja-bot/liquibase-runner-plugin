package org.jenkinsci.plugins.liquibase.evaluator;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;
import org.jenkinsci.plugins.liquibase.common.LiquibaseProperty;
import org.jenkinsci.plugins.liquibase.common.PropertiesAssembler;
import org.jenkinsci.plugins.liquibase.common.Util;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public abstract class AbstractLiquibaseBuilder extends Builder implements SimpleBuildStep {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractLiquibaseBuilder.class);

    protected String databaseEngine;
    protected String changeLogFile;
    protected String url;
    protected String defaultSchemaName;
    protected String contexts;
    protected String liquibasePropertiesPath;
    protected String labels;
    private String changeLogParameters;
    private String basePath;
    private String credentialsId;


    @Deprecated
    protected transient String username;
    @Deprecated
    protected transient String password;

    @Deprecated
    public AbstractLiquibaseBuilder(String databaseEngine,
                                    String changeLogFile,
                                    String url,
                                    String defaultSchemaName,
                                    String contexts,
                                    String liquibasePropertiesPath,
                                    String changeLogParameters,
                                    String labels,
                                    String basePath,
                                    String credentialsId) {
        this.databaseEngine = databaseEngine;
        this.changeLogFile = changeLogFile;
        this.url = url;
        this.defaultSchemaName = defaultSchemaName;
        this.contexts = contexts;
        this.liquibasePropertiesPath = liquibasePropertiesPath;
        this.changeLogParameters = changeLogParameters;
        this.labels = labels;
        this.basePath = basePath;
        this.credentialsId = credentialsId;
    }

    public AbstractLiquibaseBuilder() {

    }

    protected Object readResolve() {
        return this;
    }

    public abstract void runPerform(Run<?, ?> build,
                                    TaskListener listener,
                                    Liquibase liquibase,
                                    Contexts contexts,
                                    LabelExpression labelExpression,
                                    ExecutedChangesetAction executedChangesetAction,
                                    FilePath workspace)
            throws InterruptedException, IOException, LiquibaseException;

    abstract public Descriptor<Builder> getDescriptor();

    @Override
    public void perform(@Nonnull Run<?, ?> build,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {

        Properties configProperties = PropertiesAssembler.createLiquibaseProperties(this, build,
                build.getEnvironment(listener), workspace);
        ExecutedChangesetAction executedChangesetAction = new ExecutedChangesetAction(build);
        Liquibase liquibase =
                createLiquibase(build, listener, executedChangesetAction, configProperties, launcher, workspace);
        String liqContexts = getProperty(configProperties, LiquibaseProperty.CONTEXTS);
        Contexts contexts = new Contexts(liqContexts);
        LabelExpression labelExpression =
                new LabelExpression(getProperty(configProperties, LiquibaseProperty.LABELS));

        try {
            runPerform(build, listener, liquibase, contexts, labelExpression, executedChangesetAction, workspace);
        } catch (LiquibaseException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.UNSTABLE);
        } finally {
            closeLiquibase(liquibase);
        }
        if (!executedChangesetAction.isNoExecutionsExpected()) {
            build.addAction(executedChangesetAction);
        }
    }

    public Liquibase createLiquibase(Run<?, ?> build,
                                     TaskListener listener,
                                     ExecutedChangesetAction action,
                                     Properties configProperties,
                                     Launcher launcher, FilePath workspace) throws IOException, InterruptedException {
        Liquibase liquibase;

        boolean resolveMacros = build instanceof AbstractBuild;
        EnvVars environment = build.getEnvironment(listener);

        try {
            ClassLoader liquibaseClassLoader = this.getClass().getClassLoader();

            JdbcConnection jdbcConnection = createJdbcConnection(configProperties, liquibaseClassLoader);
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
            ResourceAccessor resourceAccessor = createResourceAccessor(workspace, environment, resolveMacros);

            String changeLogFile = getProperty(configProperties, LiquibaseProperty.CHANGELOG_FILE);
            liquibase = new Liquibase(changeLogFile, resourceAccessor, database);

        } catch (LiquibaseException e) {
            throw new RuntimeException("Error creating liquibase database.", e);
        }
        liquibase.setChangeExecListener(new BuildChangeExecListener(action, listener));

        if (!Strings.isNullOrEmpty(changeLogParameters)) {
            populateChangeLogParameters(liquibase, environment, changeLogParameters, resolveMacros);
        }
        return liquibase;
    }

    private ResourceAccessor createResourceAccessor(FilePath workspace,
                                                    Map environment,
                                                    boolean resolveMacros) {
        String resolvedBasePath;
        if (resolveMacros) {
            resolvedBasePath = hudson.Util.replaceMacro(basePath,  environment);
        } else {
            resolvedBasePath = basePath;
        }
        FilePath filePath;
        if (Strings.isNullOrEmpty(resolvedBasePath)) {
            filePath = workspace;
        } else {
            filePath = workspace.child(resolvedBasePath);
        }

        return new FilePathAccessor(filePath);
    }

    protected static void populateChangeLogParameters(Liquibase liquibase,
                                                      Map environment,
                                                      CharSequence changeLogParameters, boolean resolveMacros) {
        Map<String, String> keyValuePairs = Splitter.on("\n").trimResults().withKeyValueSeparator("=").split(changeLogParameters);
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
            String value = entry.getValue();
            String resolvedValue;
            String resolvedKey;
            String key = entry.getKey();
            if (resolveMacros) {
                resolvedValue = hudson.Util.replaceMacro(value, environment);
                resolvedKey = hudson.Util.replaceMacro(key, environment);
            } else {
                resolvedValue = value;
                resolvedKey = key;
            }
            liquibase.setChangeLogParameter(resolvedKey, resolvedValue);
        }
    }

    private static JdbcConnection createJdbcConnection(Properties configProperties, ClassLoader liquibaseClassLoader) {
        Connection connection;
        String dbUrl = getProperty(configProperties, LiquibaseProperty.URL);
        final String driverName = DatabaseFactory.getInstance().findDefaultDriver(dbUrl);
        try {
            Util.registerDatabaseDriver(driverName, liquibaseClassLoader);
            String userName = getProperty(configProperties, LiquibaseProperty.USERNAME);
            String password = getProperty(configProperties, LiquibaseProperty.PASSWORD);
            connection = DriverManager.getConnection(dbUrl, userName, password);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Error getting database connection using driver " + driverName + " using url '" + dbUrl + "'", e);
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

    public String getBasePath() {
        return basePath;
    }

    @DataBoundSetter
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void clearDatabaseEngine() {
        databaseEngine=null;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setUsername(String username) {
        this.username = username;
    }

    @DataBoundSetter
    public void setPassword(String password) {
        this.password = password;
    }

    @Deprecated
    public String getUsername() {
        return username;
    }
    @Deprecated
    public String getPassword() {
        return password;
    }

    public void clearLegacyCredentials() {
        username=null;
        password=null;
    }

    public boolean hasLegacyCredentials() {
        return !Strings.isNullOrEmpty(username);
    }
}
