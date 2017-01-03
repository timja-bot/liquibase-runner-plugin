package org.jenkinsci.plugins.liquibase.workflow;

import java.util.List;

import org.jenkinsci.plugins.liquibase.evaluator.AbstractLiquibaseBuilder;

public class LiquibaseWorkflowUtil {
    public static void setCommonConfiguration(AbstractLiquibaseBuilder builder, AbstractLiquibaseStep step) {
        builder.setChangeLogFile(step.getChangeLogFile());
        builder.setUrl(step.getUrl());
        builder.setDefaultSchemaName(step.getDefaultSchemaName());
        builder.setContexts(step.getContexts());
        builder.setLiquibasePropertiesPath(step.getLiquibasePropertiesPath());
        builder.setClasspath(step.getClasspath());
        builder.setDriverClassname(step.getDriverClassname());
        builder.setLabels(step.getLabels());
        builder.setCredentialsId(step.getCredentialsId());
        builder.setBasePath(step.getBasePath());

        if (step.getChangeLogParameters()!=null) {
            String parameterString = composeParameters(step.getChangeLogParameters());
            builder.setChangeLogParameters(parameterString);
        }
        builder.setDatabaseEngine(step.getDatabaseEngine());
        if (step.getDatabaseEngine() != null) {
            builder.setUseIncludedDriver(true);
        }
    }

    protected static String composeParameters(List<String> params) {
        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            sb.append(param).append("\n");
        }
        return sb.substring(0, sb.length() - 1);
    }
}
