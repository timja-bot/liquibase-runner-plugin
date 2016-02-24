package org.jenkinsci.plugins.liquibase.common;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.EmbeddedDriver;
import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class PropertiesParser {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesParser.class);

    /**
     * Creates a properties instance representing liquibase configuration elements.  Will first attempt to load
     * based on {@link ChangesetEvaluator#liquibasePropertiesPath}, then
     * on any elements set directly on the builder.  Therefore, those set within the project configuration take precedence
     * over those defined in the external file.
     * @param liquibaseBuilder
     * @return
     */
    public static Properties createConfigProperties(AbstractLiquibaseBuildStep liquibaseBuilder) {
        Properties properties = new Properties();

        String liquibasePropertiesPath = liquibaseBuilder.getLiquibasePropertiesPath();
        if (!Strings.isNullOrEmpty(liquibasePropertiesPath)) {
            try {
                properties.load(new FileReader(liquibasePropertiesPath));
            } catch (IOException e) {
                throw new LiquibaseRuntimeException(
                        "Unable to load properties file at[" + liquibasePropertiesPath + "] ", e);
            }
        }
        setBasedOnConfiguration(liquibaseBuilder, properties);

        return properties;
    }

    private static void setBasedOnConfiguration(AbstractLiquibaseBuildStep liquibaseBuilder, Properties properties) {

        setIfNotNull(properties, LiquibaseProperty.USERNAME, liquibaseBuilder.getUsername());
        setIfNotNull(properties, LiquibaseProperty.DEFAULT_SCHEMA_NAME, liquibaseBuilder.getDefaultSchemaName());
        setIfNotNull(properties, LiquibaseProperty.PASSWORD, liquibaseBuilder.getPassword());
        setIfNotNull(properties, LiquibaseProperty.URL, liquibaseBuilder.getUrl());
        setIfNotNull(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile());
        setIfNotNull(properties, LiquibaseProperty.CONTEXTS, liquibaseBuilder.getContexts());
    }

    private static void setIfNotNull(Properties properties,
                                     LiquibaseProperty liquibaseProperty,
                                     String value) {
        if (!Strings.isNullOrEmpty(value)) {
            properties.setProperty(liquibaseProperty.getOption(), value);
        }

    }

    public static void setDriverFromDBEngine(ChangesetEvaluator changesetEvaluator, Properties properties) {
        if (!Strings.isNullOrEmpty(changesetEvaluator.getDatabaseEngine())) {
            for (EmbeddedDriver embeddedDriver : changesetEvaluator.getDrivers()) {
                if (embeddedDriver.getDisplayName().equals(changesetEvaluator.getDatabaseEngine())) {
                    properties.setProperty(LiquibaseProperty.DRIVER.getOption(), embeddedDriver.getDriverClassName());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("using db driver class[" + embeddedDriver.getDriverClassName() + "] ");
                    }
                    break;
                }
            }
        }
    }

}
