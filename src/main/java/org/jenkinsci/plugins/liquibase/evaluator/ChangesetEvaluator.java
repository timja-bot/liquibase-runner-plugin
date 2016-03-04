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
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.ResourceAccessor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nullable;

import org.jenkinsci.plugins.liquibase.common.LiquibaseCommand;
import org.jenkinsci.plugins.liquibase.common.LiquibaseProperty;
import org.jenkinsci.plugins.liquibase.common.PropertiesParser;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
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

    protected static final String DEFAULT_LOGLEVEL = "info";

    protected static final String OPTION_HYPHENS = "--";
    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluator.class);

    protected String databaseEngine;
    protected String changeLogFile;
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
    protected String liquibasePropertiesPath;
    private boolean invokeExternal;
    private boolean dropAll;
    private String command;
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
        this.command = command;
        this.classpath = classpath;
        this.driverClassname = driverClassname;
        this.labels = labels;
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


    public Liquibase createLiquibase(AbstractBuild<?, ?> build,
                                     BuildListener listener,
                                     ExecutedChangesetAction action,
                                     Properties configProperties, Launcher launcher) {
        Liquibase liquibase;

        String driverName = getProperty(configProperties, LiquibaseProperty.DRIVER);

        try {
            Connection connection = getDatabaseConnection(configProperties, driverName);
            JdbcConnection jdbcConnection = new JdbcConnection(connection);
            Database database =
                    DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

            ResourceAccessor resourceAccessor = createResourceAccessor(build, launcher);
            liquibase = new Liquibase(configProperties.getProperty(LiquibaseProperty.CHANGELOG_FILE.getOption()),
                    resourceAccessor, database);


        } catch (LiquibaseException e) {
            throw new RuntimeException("Error creating liquibase database.", e);
        }
        liquibase.setChangeExecListener(new BuildChangeExecListener(action, listener));
        return liquibase;
    }

    protected ResourceAccessor createResourceAccessor(AbstractBuild<?, ?> build, Launcher launcher) {
        ResourceAccessor resourceAccessor;
        if (!Strings.isNullOrEmpty(classpath)) {
            String separator;
            if (launcher.isUnix()) {
                separator = ":";
            } else {
                separator = ";";
            }
            Iterable<String> classPathElements = Splitter.on(separator).trimResults().split(classpath);
            Iterator<String> iterator = classPathElements.iterator();
            final Iterable<URL> urlIterable = Iterables.transform(classPathElements, new Function<String, URL>() {
                @Override
                public URL apply(@Nullable String s) {
                    URL url = null;
                    try {
                        url = new File(s).toURI().toURL();
                    } catch (MalformedURLException e) {
                        LOG.warn("Unable to transform classpath element " + s, e);
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
            resourceAccessor =
                    new CompositeResourceAccessor(new FilePathAccessor(build), new ClassLoaderResourceAccessor());
        } else {
            resourceAccessor = new FilePathAccessor(build);
        }
        return resourceAccessor;
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

    public List<IncludedDatabaseDriver> getDrivers() {
        return DESCRIPTOR.getIncludedDatabaseDrivers();
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

    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public String getCommand() {
        return command;
    }

    @DataBoundSetter
    public void setCommand(String command) {
        this.command = command;
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
        Properties configProperties = PropertiesParser.createConfigProperties(this);
        ExecutedChangesetAction action = new ExecutedChangesetAction(build);
        Liquibase liquibase = createLiquibase(build, listener, action, configProperties, launcher);
        String liqContexts = getProperty(configProperties, LiquibaseProperty.CONTEXTS);

        try {
            String resolvedCommand;

            if (!Strings.isNullOrEmpty(command)) {
                resolvedCommand = this.command;
            } else {
                if (isTestRollbacks()) {
                    resolvedCommand = LiquibaseCommand.UPDATE_TESTING_ROLLBACKS.getCommand();
                } else {
                    resolvedCommand = LiquibaseCommand.UPDATE.getCommand();
                }
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

    public void setChangeLogFile(String changeLogFile) {
        this.changeLogFile = changeLogFile;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDefaultSchemaName(String defaultSchemaName) {
        this.defaultSchemaName = defaultSchemaName;
    }

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