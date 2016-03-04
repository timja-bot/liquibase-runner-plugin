package org.jenkinsci.plugins.liquibase.common;

import hudson.tasks.Builder;

/**
 * Common configuration for each type of liquibase builder.
 */
public abstract class AbstractLiquibaseBuildStep extends Builder {


    public AbstractLiquibaseBuildStep() {

    }

    public AbstractLiquibaseBuildStep(String url,
                                      String password,
                                      String changeLogFile,
                                      String username,
                                      String defaultSchemaName,
                                      String liquibasePropertiesPath,
                                      boolean testRollbacks,
                                      String contexts) {


    }

}
