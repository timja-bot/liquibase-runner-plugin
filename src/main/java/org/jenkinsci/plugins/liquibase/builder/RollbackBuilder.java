package org.jenkinsci.plugins.liquibase.builder;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

/**
 * Build step that invoke's liquibase's rollback against a target database.
 */
public class RollbackBuilder extends AbstractLiquibaseBuilder {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private String rollbackType;
    protected String numberOfChangesetsToRollback;
    private String rollbackLastHours;
    private String rollbackToTag;
    private String rollbackToDate;

    private transient SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN);

    public enum RollbackStrategy {
        TAG, DATE, RELATIVE, COUNT
    }

    @DataBoundConstructor
    public RollbackBuilder() {
    }

    @Override
    protected void addCommandAndArguments(ArgumentListBuilder cliCommand, Properties configProperties, Run<?, ?> build, TaskListener listener) throws IOException {
        try {
            RollbackStrategy rollbackStrategy = RollbackStrategy.valueOf(rollbackType);
            EnvVars environment = build.getEnvironment(listener);

            if (rollbackStrategy == RollbackStrategy.COUNT) {
                String resolvedRollbackCount = Util.replaceMacro(numberOfChangesetsToRollback, environment);
                int rollbackCount;
                if (resolvedRollbackCount != null) {
                    rollbackCount = Integer.parseInt(resolvedRollbackCount);
                } else {
                    throw new LiquibaseRuntimeException(
                            "Invalid value '" + numberOfChangesetsToRollback + "' for rollback count.");
                }

                cliCommand.add("rollbackCount", String.valueOf(rollbackCount));
            }

            if (rollbackStrategy == RollbackStrategy.DATE || rollbackStrategy == RollbackStrategy.RELATIVE) {
                Date targetDate = resolveTargetDate(rollbackStrategy, environment);

                cliCommand.add("rollbackDate", String.valueOf(targetDate));
            }

            if (rollbackStrategy == RollbackStrategy.TAG) {
                String resolvedTag = Util.replaceMacro(rollbackToTag, environment);
                cliCommand.add("rollback", resolvedTag);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    protected Date resolveTargetDate(RollbackStrategy rollbackStrategy, EnvVars environment) {
        Date now = new Date();
        return resolveTargetDate(rollbackStrategy, now, environment);
    }

    protected Date resolveTargetDate(RollbackStrategy rollbackStrategy, Date now, EnvVars environment) {
        Date targetDate;
        if (rollbackStrategy == RollbackStrategy.RELATIVE) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(now);

            String lastHours = Util.replaceMacro(rollbackLastHours, environment);
            if (lastHours != null) {
                instance.add(Calendar.HOUR, 0 - Integer.parseInt(lastHours));
            }
            targetDate = instance.getTime();
        } else {
            String rollbackDate = Util.replaceMacro(rollbackToDate, environment);
            try {
                targetDate = simpleDateFormat.parse(rollbackDate);
            } catch (ParseException e) {
                throw new LiquibaseRuntimeException("Invalid value for rollback to date value:" + rollbackDate,
                        e);
            }
        }
        return targetDate;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public String getNumberOfChangesetsToRollback() {
        return numberOfChangesetsToRollback;
    }

    @DataBoundSetter
    public void setNumberOfChangesetsToRollback(String numberOfChangesetsToRollback) {
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

    @Extension
    public static class DescriptorImpl extends AbstractLiquibaseDescriptor {

        public DescriptorImpl() {
            load();
        }

        public DescriptorImpl(Class<? extends RollbackBuilder> clazz) {
            super(clazz);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Liquibase: Roll Back Changes";
        }

    }
}
