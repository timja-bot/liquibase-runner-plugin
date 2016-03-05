package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Extension;
import hudson.FilePath;
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nullable;

import org.jenkinsci.plugins.liquibase.common.DriverShim;
import org.jenkinsci.plugins.liquibase.common.LiquibaseCommand;
import org.jenkinsci.plugins.liquibase.common.LiquibaseProperty;
import org.jenkinsci.plugins.liquibase.common.PropertiesAssembler;
import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
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

    private ChangesetEvaluator(ChangesetEvaluatorBuilder changesetEvaluatorBuilder) {
        databaseEngine = changesetEvaluatorBuilder.databaseEngine;

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


    public Liquibase createLiquibase(AbstractBuild<?, ?> build,
                                     BuildListener listener,
                                     ExecutedChangesetAction action,
                                     Properties configProperties, Launcher launcher) {
        Liquibase liquibase;
        String driverName = getProperty(configProperties, LiquibaseProperty.DRIVER);

        try {
            if (!Strings.isNullOrEmpty(classpath)) {
                addClassloader(launcher.isUnix(), build.getWorkspace());
            }

            Connection connection = getDatabaseConnection(configProperties, driverName);
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
        return liquibase;
    }

    private void addClassloader(boolean isUnix, final FilePath workspace) {
        String separator;
        if (isUnix) {
            separator = ":";
        } else {
            separator = ";";
        }
        Iterable<String> classPathElements = Splitter.on(separator).trimResults().split(classpath);
        final Iterable<URL> urlIterable = Iterables.transform(classPathElements, new Function<String, URL>() {
            @Override
            public URL apply(@Nullable String filePath) {
                URL url = null;
                if (filePath != null) {
                    try {
                        if (Paths.get(filePath).isAbsolute()) {
                            url = new File(filePath).toURI().toURL();
                        } else {
                            URI workspaceUri = workspace.toURI();
                            File workspace = new File(workspaceUri);
                            url = new File(workspace, filePath).toURI().toURL();
                        }
                    } catch (MalformedURLException e) {
                        LOG.warn("Unable to transform classpath element " + filePath, e);
                    } catch (InterruptedException e) {
                        throw new LiquibaseRuntimeException("Error during database driver resolution", e);
                    } catch (IOException e) {
                        throw new LiquibaseRuntimeException("Error during database driver resolution", e);
                    }
                }
                return url;
            }
        });

        URLClassLoader urlClassLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                return new URLClassLoader(Iterables.toArray(urlIterable, URL.class),
                        Thread.currentThread().getContextClassLoader());
            }
        });
        Thread.currentThread().setContextClassLoader(urlClassLoader);

    }

    protected static Connection getDatabaseConnection(Properties configProperties, String driverName) {
        Connection connection;
        String dbUrl = getProperty(configProperties, LiquibaseProperty.URL);
        try {
            Driver actualDriver =
                    (Driver) Class.forName(driverName, true, Thread.currentThread().getContextClassLoader())
                                  .newInstance();
            Driver driverShim = new DriverShim(actualDriver);

            DriverManager.registerDriver(driverShim);
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

    public boolean isTestRollbacks() {
        return testRollbacks;
    }

    public boolean isDropAll() {
        return dropAll;
    }

    public String getLabels() {
        return labels;
    }

    @DataBoundSetter
    public void setLabels(String labels) {
        this.labels = labels;
    }

    @DataBoundSetter
    public void setDropAll(boolean dropAll) {
        this.dropAll = dropAll;
    }

    public String getDatabaseEngine() {
        return databaseEngine;
    }

    @DataBoundSetter
    public void setTestRollbacks(boolean testRollbacks) {
        this.testRollbacks = testRollbacks;
    }

    public String getDriverClassname() {
        return driverClassname;
    }

    @DataBoundSetter
    public void setDriverClassname(String driverClassname) {
        this.driverClassname = driverClassname;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        Properties configProperties = PropertiesAssembler.createLiquibaseProperties(this);
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
                liquibase.dropAll();
            }
            listener.getLogger().println("Running liquibase command '" + resolvedCommand + "'");

            if (LiquibaseCommand.UPDATE_TESTING_ROLLBACKS.isCommand(resolvedCommand)) {
                liquibase.updateTestingRollback(new Contexts(contexts), new liquibase.LabelExpression(labels));
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

    public String getChangeLogFile() {
        return changeLogFile;
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

    public String getLiquibasePropertiesPath() {
        return liquibasePropertiesPath;
    }

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

    @DataBoundSetter
    public void setChangeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    @DataBoundSetter
    public void setUsername(String username) {
        this.username = username;
    }

    @DataBoundSetter
    public void setPassword(String password) {
        this.password = password;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    @DataBoundSetter
    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

    @DataBoundSetter
    public void setContexts(String contexts) {
        this.contexts = contexts;
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


    public static final class ChangesetEvaluatorBuilder {
        private String databaseEngine;
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