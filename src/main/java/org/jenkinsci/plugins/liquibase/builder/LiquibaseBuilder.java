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
public class LiquibaseBuilder extends AbstractLiquibaseBuildStep {
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

    protected String databaseEngine;
    private boolean invokeExternal;

    @DataBoundConstructor
    public LiquibaseBuilder(String url,
                            String password,
                            String changeLogFile,
                            String username,
                            String defaultSchemaName,
                            String liquibasePropertiesPath,
                            boolean testRollbacks,
                            String contexts,
                            String databaseEngine,
                            String installationName) {
        super(url, password, changeLogFile, username, defaultSchemaName, liquibasePropertiesPath, testRollbacks,
                contexts);
        this.databaseEngine = databaseEngine;
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

    public boolean isTestRollbacks() {
        return testRollbacks;
    }

    public void setInvokeExternal(boolean invokeExternal) {
        this.invokeExternal = invokeExternal;
    }

    public boolean isInvokeExternal() {
        return invokeExternal;
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

        public DescriptorImpl(Class<? extends LiquibaseBuilder> clazz) {
            super(clazz);
        }

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

        public void setInstallations(LiquibaseInstallation... installations) {
            this.installations = installations;
            save();
        }
    }

}