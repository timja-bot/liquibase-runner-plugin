package org.jenkinsci.plugins.liquibase.workflow;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.liquibase.evaluator.RollbackBuilder;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import com.google.inject.Inject;

public class RollbackStepExecution extends AbstractSynchronousStepExecution<Void> {
    @Inject
    private transient RollbackStep step;

    @StepContextParameter
    private transient TaskListener listener;

    @StepContextParameter
    private transient Launcher launcher;

    @StepContextParameter
    private transient Run<?, ?> run;

    @StepContextParameter
    private transient FilePath ws;

    @StepContextParameter
    private transient EnvVars envVars;


    @Override
    protected Void run() throws Exception {
        RollbackBuilder rollbackBuildStep  = new RollbackBuilder();
        LiquibaseWorkflowUtil.setCommonConfiguration(rollbackBuildStep, step);

        if (step.getRollbackCount() != 0) {
            rollbackBuildStep.setNumberOfChangesetsToRollback(String.valueOf(step.getRollbackCount()));
            rollbackBuildStep.setRollbackType(RollbackBuilder.RollbackStrategy.COUNT.name());
        }
        if (step.getRollbackToDate() != null) {
            rollbackBuildStep.setRollbackToDate(step.getRollbackToDate());
            rollbackBuildStep.setRollbackType(RollbackBuilder.RollbackStrategy.DATE.name());
        }
        if (step.getRollbackToTag() != null) {
            rollbackBuildStep.setRollbackToTag(step.getRollbackToTag());
            rollbackBuildStep.setRollbackType(RollbackBuilder.RollbackStrategy.TAG.name());
        }
        if (step.getRollbackLastHours() != null) {
            rollbackBuildStep.setRollbackLastHours(step.getRollbackLastHours());
            rollbackBuildStep.setRollbackType(RollbackBuilder.RollbackStrategy.RELATIVE.name());
        }

        rollbackBuildStep.perform(run, ws, launcher, listener);

        return null;
    }
}
