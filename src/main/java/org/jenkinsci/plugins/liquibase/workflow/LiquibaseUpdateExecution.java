package org.jenkinsci.plugins.liquibase.workflow;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import com.google.inject.Inject;

public class LiquibaseUpdateExecution extends AbstractSynchronousStepExecution<Void> {

    @Inject
    private transient LiquibaseUpdateBuildStep step;

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
        ChangesetEvaluator changesetEvaluator = new ChangesetEvaluator();
        LiquibaseWorkflowUtil.setCommonConfiguration(changesetEvaluator, step);

        changesetEvaluator.setTestRollbacks(step.isTestRollbacks());
        changesetEvaluator.setDropAll(step.isDropAll());
        changesetEvaluator.setTagOnSuccessfulBuild(step.isTagOnSuccessfulBuild());

        changesetEvaluator.perform(run, ws, launcher, listener);
        return null;

    }

}
