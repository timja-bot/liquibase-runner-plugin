package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Build step that invoke's liquibase's rollback against a target database.
 */
public class RollbackBuildStep extends AbstractLiquibaseBuilder {
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private String rollbackType;
    protected int numberOfChangesetsToRollback;
    private String rollbackLastHours;
    private String rollbackToTag;
    private String rollbackToDate;


    private SimpleDateFormat simpleDateFormat= new SimpleDateFormat(DATE_PATTERN);

    public enum RollbackStrategy {
        TAG, DATE, RELATIVE, COUNT
    }

    public RollbackBuildStep() {


    }

    @DataBoundConstructor
    public RollbackBuildStep(String databaseEngine,
                             String changeLogFile,
                             String username,
                             String password,
                             String url,
                             String defaultSchemaName,
                             String contexts,
                             String liquibasePropertiesPath,
                             String classpath,
                             String driverClassname,
                             String changeLogParameters,
                             String labels,
                             String rollbackType,
                             int numberOfChangesetsToRollback,
                             String rollbackLastHours,
                             String rollbackToTag, String rollbackToDate) {
        super(databaseEngine, changeLogFile, username, password, url, defaultSchemaName, contexts,
                liquibasePropertiesPath,
                classpath, driverClassname, changeLogParameters, labels);

        this.rollbackType = rollbackType;
        this.numberOfChangesetsToRollback = numberOfChangesetsToRollback;
        this.rollbackLastHours = rollbackLastHours;
        this.rollbackToTag = rollbackToTag;
        this.rollbackToDate = rollbackToDate;
    }

    @Override
    public void doPerform(AbstractBuild<?, ?> build,
                          BuildListener listener,
                          Liquibase liquibase,
                          Contexts contexts,
                          ExecutedChangesetAction executedChangesetAction, Properties configProperties)
            throws InterruptedException, IOException, LiquibaseException {

        executedChangesetAction.setRollbackOnly(true);
        RolledbackChangesetAction action = new RolledbackChangesetAction(build);
        RollbackStrategy rollbackStrategy = RollbackStrategy.valueOf(rollbackType);
        build.addAction(action);

        if (rollbackStrategy == RollbackStrategy.COUNT) {
            listener.getLogger().println("Rollback back the last " + numberOfChangesetsToRollback + " changeset(s) applied.");
            liquibase.rollback(numberOfChangesetsToRollback, contexts, new LabelExpression(labels));
        }

        if (rollbackStrategy == RollbackStrategy.DATE || rollbackStrategy == RollbackStrategy.RELATIVE) {
            Date targetDate = resolveTargetDate(rollbackStrategy);
            listener.getLogger().println("Rolling back changeset(s) applied after date " + simpleDateFormat.format(targetDate));
            liquibase.rollback(targetDate, contexts, new LabelExpression(labels));
        }

        if (rollbackStrategy == RollbackStrategy.TAG) {
            listener.getLogger().println("Rolling back database to tag '" + rollbackToTag+ "'");
            liquibase.rollback(rollbackToTag, contexts, new LabelExpression(labels));
        }

        action.setRolledbackChangesets(executedChangesetAction.getRolledBackChangesets());
    }


    protected Date resolveTargetDate(RollbackStrategy rollbackStrategy) {
        Date now = new Date();
        return resolveTargetDate(rollbackStrategy, now);
    }

    protected Date resolveTargetDate(RollbackStrategy rollbackStrategy, Date now) {
        Date targetDate;
        if (rollbackStrategy == RollbackStrategy.RELATIVE) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(now);

            instance.add(Calendar.HOUR, 0 - Integer.parseInt(rollbackLastHours));
            targetDate = instance.getTime();
        } else {
            try {
                targetDate = simpleDateFormat.parse(rollbackToDate);
            } catch (ParseException e) {
                throw new LiquibaseRuntimeException("Invalid value for rollback to date value:" + rollbackToDate,
                        e);
            }
        }
        return targetDate;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public int getNumberOfChangesetsToRollback() {
        return numberOfChangesetsToRollback;
    }

    @DataBoundSetter
    public void setNumberOfChangesetsToRollback(int numberOfChangesetsToRollback) {
        this.numberOfChangesetsToRollback = numberOfChangesetsToRollback;
    }

    public String getRollbackToTag() {
        return rollbackToTag;
    }

    @DataBoundSetter
    public void setRollbackToTag(String rollbackToTag) {
        this.rollbackToTag = rollbackToTag;
    }

    public String getRollbackToDate() {
        return rollbackToDate;
    }

    @DataBoundSetter
    public void setRollbackToDate(String rollbackToDate) {
        this.rollbackToDate = rollbackToDate;
    }

    public String getRollbackType() {
        return rollbackType;
    }

    @DataBoundSetter
    public void setRollbackType(String rollbackType) {
        this.rollbackType = rollbackType;
    }


    public String getRollbackLastHours() {
        return rollbackLastHours;
    }

    @DataBoundSetter
    public void setRollbackLastHours(String rollbackLastHours) {
        this.rollbackLastHours = rollbackLastHours;
    }

    public static class DescriptorImpl extends AbstractLiquibaseDescriptor {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Roll back liquibase changes";
        }

    }

}
