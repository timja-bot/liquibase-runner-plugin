package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.Builder;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.MigrationFailedException;

import java.io.IOException;

import org.jenkinsci.plugins.liquibase.common.LiquibaseCommand;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jenkins builder which evaluates liquibase changesets.
 */
@SuppressWarnings("ProhibitedExceptionThrown")
public class ChangesetEvaluator extends AbstractLiquibaseBuilder {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger LOG = LoggerFactory.getLogger(ChangesetEvaluator.class);

    protected boolean testRollbacks;
    private boolean dropAll;
    private String liquibaseOperation;
    protected int changesToRollback = 1;

    public ChangesetEvaluator() {
        super();
    }

    @DataBoundConstructor
    public ChangesetEvaluator(String databaseEngine,
                              String changeLogFile,
                              String username,
                              String password,
                              String url,
                              String defaultSchemaName,
                              String contexts,
                              String liquibasePropertiesPath,
                              String classpath,
                              String driverClassname,
                              String changeLogParameters, boolean testRollbacks, boolean dropAll, String labels) {
        super(databaseEngine, changeLogFile, username, password, url, defaultSchemaName, contexts,
                liquibasePropertiesPath,
                classpath, driverClassname, changeLogParameters, labels);
        this.testRollbacks = testRollbacks;
        this.dropAll = dropAll;
    }

    @Override
    public void doPerform(AbstractBuild<?, ?> build,
                          BuildListener listener,
                          Liquibase liquibase,
                          Contexts contexts,
                          RolledbackChangesetAction rolledbackChangesetAction,
                          ExecutedChangesetAction executedChangesetAction)
            throws InterruptedException, IOException {

        executedChangesetAction.setProvideStatusIfEmpty(true);
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
                liquibase.updateTestingRollback(contexts, new liquibase.LabelExpression(labels));
            }

            if (LiquibaseCommand.UPDATE.isCommand(resolvedCommand)) {
                liquibase.update(contexts, new liquibase.LabelExpression(labels));
            }

            if (LiquibaseCommand.ROLLBACK.isCommand(resolvedCommand)) {
                liquibase.rollback(changesToRollback, contexts.toString());
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
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public boolean isTestRollbacks() {
        return testRollbacks;
    }


    @DataBoundSetter
    public void setTestRollbacks(boolean testRollbacks) {
        this.testRollbacks = testRollbacks;
    }

    public boolean isDropAll() {
        return dropAll;
    }

    @DataBoundSetter
    public void setDropAll(boolean dropAll) {
        this.dropAll = dropAll;
    }

    public String getLiquibaseOperation() {
        return liquibaseOperation;
    }

    @DataBoundSetter
    public void setLiquibaseOperation(String liquibaseOperation) {
        this.liquibaseOperation = liquibaseOperation;
    }

    public static class DescriptorImpl extends AbstractLiquibaseDescriptor {


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
    }

}