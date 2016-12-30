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
        changesetEvaluator.setDatabaseEngine(step.getDatabaseEngine());
        changesetEvaluator.setChangeLogFile(step.getChangeLogFile());
        changesetEvaluator.setUrl(step.getUrl());
        changesetEvaluator.setDefaultSchemaName(step.getDefaultSchemaName());
        changesetEvaluator.setContexts(step.getContexts());
        changesetEvaluator.setLiquibasePropertiesPath(step.getLiquibasePropertiesPath());
        changesetEvaluator.setClasspath(step.getClasspath());
        changesetEvaluator.setDriverClassname(step.getDriverClassname());
        changesetEvaluator.setLabels(step.getLabels());
        changesetEvaluator.setChangeLogParameters(step.getChangeLogParameters());
        changesetEvaluator.setBasePath(step.getBasePath());
        changesetEvaluator.setUseIncludedDriver(step.getUseIncludedDriver());


        changesetEvaluator.setTestRollbacks(step.isTestRollbacks());
        changesetEvaluator.setDropAll(step.isDropAll());
        changesetEvaluator.setTagOnSuccessfulBuild(step.isTagOnSuccessfulBuild());


        changesetEvaluator.perform(run, ws, launcher, listener);
        return null;

    }
}
