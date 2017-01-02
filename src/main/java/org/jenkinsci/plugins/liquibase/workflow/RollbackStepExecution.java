package org.jenkinsci.plugins.liquibase.workflow;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.liquibase.evaluator.RollbackBuildStep;
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
        RollbackBuildStep rollbackBuildStep  = new RollbackBuildStep();
        LiquibaseWorkflowUtil.setCommonConfiguration(rollbackBuildStep, step);

        rollbackBuildStep.setNumberOfChangesetsToRollback(String.valueOf(step.getRollbackCount()));
        rollbackBuildStep.setRollbackToTag(step.getRollbackToTag());

        rollbackBuildStep.perform(run, ws, launcher, listener);

        return null;
    }
}
