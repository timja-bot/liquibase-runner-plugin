package org.jenkinsci.plugins.liquibase.workflow;

import org.jenkinsci.plugins.liquibase.evaluator.AbstractLiquibaseBuilder;

public class LiquibaseWorkflowUtil {
    public static void setCommonConfiguration(AbstractLiquibaseBuilder changesetEvaluator, AbstractLiquibaseStep step) {
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
        if (step.getDatabaseEngine() != null) {
            changesetEvaluator.setUseIncludedDriver(true);
        }
        changesetEvaluator.setCredentialsId(step.getCredentialsId());
    }
}
