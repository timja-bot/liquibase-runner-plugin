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
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.MigrationFailedException;
import liquibase.integration.commandline.CommandLineUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

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

    private String driverName;

    private String liquibasePropertiesPath;

    private boolean invokeExternal;

    private String installationName;


    @DataBoundConstructor
    public LiquibaseBuilder(String changeLogFile,
                            String username,
                            String password,
                            String url,
                            String defaultSchemaName,
                            String contexts,
                            String databaseEngine,
                            boolean testRollbacks,
                            String liquibasePropertiesPath,
                            boolean invokeExternal,
                            String installationName) {
        this.password = password;
        this.defaultSchemaName = defaultSchemaName;
        this.url = url;
        this.username = username;

        this.changeLogFile = changeLogFile;
        this.contexts = contexts;

        this.databaseEngine = databaseEngine;
        this.testRollbacks = testRollbacks;
        this.liquibasePropertiesPath = liquibasePropertiesPath;

        this.invokeExternal = invokeExternal;
        this.installationName = installationName;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        Properties configProperties = PropertiesParser.createConfigProperties(this);
        ExecutedChangesetAction action = new ExecutedChangesetAction(build);

        Liquibase liquibase = createLiquibase(build, listener, action, configProperties);
        String liqContexts = configProperties.getProperty(LiquibaseProperty.CONTEXTS.getOption());
        try {
            if (testRollbacks) {
                liquibase.updateTestingRollback(liqContexts);
            } else {
                liquibase.update(new Contexts(liqContexts));
            }
            build.addAction(action);
        } catch (MigrationFailedException migrationException) {
            Optional<ChangeSet> changeSetOptional = reflectFailed(migrationException);
            if (changeSetOptional.isPresent()) {
                action.addFailed(changeSetOptional.get());
            }
            migrationException.printStackTrace(listener.getLogger());
            build.setResult(Result.UNSTABLE);
        } catch (DatabaseException e) {
            e.printStackTrace(listener.getLogger());
            build.setResult(Result.FAILURE);
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
        return true;
    }

    private Liquibase createLiquibase(AbstractBuild<?, ?> build,
                                      BuildListener listener,
                                      ExecutedChangesetAction action,
                                      Properties configProperties) {

        Liquibase liquibase;
        try {
            Database databaseObject = CommandLineUtils.createDatabaseObject(getClass().getClassLoader(),
                    configProperties.getProperty(LiquibaseProperty.URL.getOption()),
                    configProperties.getProperty(LiquibaseProperty.USERNAME.getOption()),
                    configProperties.getProperty(LiquibaseProperty.PASSWORD.getOption()),
                    configProperties.getProperty(LiquibaseProperty.DRIVER.getOption()),
                    configProperties.getProperty(LiquibaseProperty.DEFAULT_CATALOG_NAME.getOption()),
                    configProperties.getProperty(LiquibaseProperty.DEFAULT_SCHEMA_NAME.getOption()), true, true, null,
                    null, null, null);

            liquibase = new Liquibase(configProperties.getProperty(LiquibaseProperty.CHANGELOG_FILE.getOption()),
                    new FilePathAccessor(build), databaseObject);
        } catch (LiquibaseException e) {
            throw new RuntimeException("Error creating liquibase database.", e);
        }
        liquibase.setChangeExecListener(new BuildChangeExecListener(action, listener));
        return liquibase;
    }

    public List<EmbeddedDriver> getDrivers() {
        return DESCRIPTOR.getEmbeddedDrivers();
    }

    /**
     * Reflection necessary because failedChangeset has private access on {@link liquibase.exception.MigrationFailedException}.
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

    public boolean isInvokeExternal() {
        return invokeExternal;
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

    public String getLiquibasePropertiesPath() {
        return liquibasePropertiesPath;
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

    public void setTestRollbacks(boolean testRollbacks) {
        this.testRollbacks = testRollbacks;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private List<EmbeddedDriver> embeddedDrivers;
        private LiquibaseInstallation[] installations;

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
        public LiquibaseInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(LiquibaseInstallation.DescriptorImpl.class);
        }

        private void initDriverList() {
            embeddedDrivers = Lists.newArrayList(new EmbeddedDriver("MySQL", "com.mysql.jdbc.Driver"),
                    new EmbeddedDriver("PostgreSQL", "org.postgresql.Driver"),
                    new EmbeddedDriver("Hypersonic", "org.hsqldb.jdbcDriver"),
                    new EmbeddedDriver("H2", "org.h2.Driver"));
        }

        public LiquibaseInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(LiquibaseInstallation[] installations) {
            this.installations = installations;
            save();
        }
    }

}