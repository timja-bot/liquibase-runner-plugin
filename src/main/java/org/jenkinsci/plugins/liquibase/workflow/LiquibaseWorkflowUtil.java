package org.jenkinsci.plugins.liquibase.workflow;

import hudson.Util;

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

        String parameterList = composeParameters(step);
        if (parameterList != null) {
            builder.setChangeLogParameters(parameterList);
        }
        builder.setDatabaseEngine(step.getDatabaseEngine());
        if (step.getDatabaseEngine() != null) {
            builder.setUseIncludedDriver(true);
        }
    }

    protected static String composeParameters(AbstractLiquibaseStep step) {

        List<String> params = step.getChangeLogParameterList();
        String fromList = "";
        if (params !=null && !params.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String param : params) {
                sb.append(param).append("\n");
            }
            if (sb.length() > 0) {
                fromList = sb.substring(0, sb.length() - 1);
            }
        }
        String result;
        if (step.getChangeLogParameters() != null) {
            result = step.getChangeLogParameters() + "\n" + fromList;
        } else {
            result = fromList;
        }
        return Util.fixEmptyAndTrim(result);
    }
}
