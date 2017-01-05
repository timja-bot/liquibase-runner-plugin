package org.jenkinsci.plugins.liquibase.workflow;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.liquibase.evaluator.DatabaseDocBuilder;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import com.google.inject.Inject;

public class DbDocExecution extends AbstractSynchronousStepExecution<Void> {

    @Inject
    private transient DbDocBuildStep step;

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
        DatabaseDocBuilder builder = new DatabaseDocBuilder(step.getOutputDirectory());
        LiquibaseWorkflowUtil.setCommonConfiguration(builder, step);
        builder.perform(run, ws, launcher, listener);

        return null;
    }
}
