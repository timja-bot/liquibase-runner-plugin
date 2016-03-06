package org.jenkinsci.plugins.liquibase.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.IncludedDatabaseDriver;
import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class PropertiesAssembler {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesAssembler.class);

    /**
     * Creates a properties instance for use with liquibase execution.
     * @param liquibaseBuilder
     * @return
     */
    public static Properties createLiquibaseProperties(ChangesetEvaluator liquibaseBuilder) {
        Properties properties = new Properties();
        assembleDefaults(liquibaseBuilder, properties);
        assembleFromProjectConfiguration(liquibaseBuilder, properties);
        return properties;
    }

    protected static void assembleFromProjectConfiguration(ChangesetEvaluator liquibaseBuilder, Properties properties) {
        String liquibasePropertiesPath = liquibaseBuilder.getLiquibasePropertiesPath();
        readFromExternalProperties(properties, liquibasePropertiesPath);
        setIfNotNull(properties, LiquibaseProperty.USERNAME, liquibaseBuilder.getUsername());
        setIfNotNull(properties, LiquibaseProperty.PASSWORD, liquibaseBuilder.getPassword());
        setIfNotNull(properties, LiquibaseProperty.DEFAULT_SCHEMA_NAME, liquibaseBuilder.getDefaultSchemaName());
        setIfNotNull(properties, LiquibaseProperty.URL, liquibaseBuilder.getUrl());
        setIfNotNull(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile());
        setIfNotNull(properties, LiquibaseProperty.LABELS, liquibaseBuilder.getLabels());
        setIfNotNull(properties, LiquibaseProperty.CONTEXTS, liquibaseBuilder.getContexts());
        resolveDatabaseDriver(liquibaseBuilder, properties);
    }

    private static void resolveDatabaseDriver(ChangesetEvaluator liquibaseBuilder, Properties properties) {
        if (!Strings.isNullOrEmpty(liquibaseBuilder.getDatabaseEngine())) {
            PropertiesAssembler.setDriverFromDBEngine(liquibaseBuilder, properties);
        } else {
            if (!Strings.isNullOrEmpty(liquibaseBuilder.getDriverClassname())) {
                setProperty(properties, LiquibaseProperty.DRIVER, liquibaseBuilder.getDriverClassname());
            }
        }
    }

    private static void readFromExternalProperties(Properties properties, String liquibasePropertiesPath) {
        if (!Strings.isNullOrEmpty(liquibasePropertiesPath)) {
            InputStreamReader streamReader = null;
            try {
                streamReader = new InputStreamReader(new FileInputStream(liquibasePropertiesPath),
                        StandardCharsets.UTF_8);
                properties.load(streamReader);
            } catch (IOException e) {
                throw new LiquibaseRuntimeException(
                        "Unable to load properties file at '" + liquibasePropertiesPath + "'", e);
            } finally {
                IOUtils.closeQuietly(streamReader);
            }
        }
    }

    private static void assembleDefaults(ChangesetEvaluator liquibaseBuilder, Properties properties) {
        setProperty(properties, LiquibaseProperty.DRIVER, "org.h2.Driver");
        setProperty(properties, LiquibaseProperty.URL, "jdbc:h2:mem:builder-db");
        setProperty(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile());
    }

    private static void setProperty(Properties properties, LiquibaseProperty liquibaseProperty, String value) {
        properties.setProperty(liquibaseProperty.propertyName(), value);
    }

    private static void setIfNotNull(Properties properties,
                                     LiquibaseProperty liquibaseProperty,
                                     String value) {
        if (!Strings.isNullOrEmpty(value)) {
            setProperty(properties, liquibaseProperty, value);
        }
    }

    public static void setDriverFromDBEngine(ChangesetEvaluator changesetEvaluator, Properties properties) {
        if (!Strings.isNullOrEmpty(changesetEvaluator.getDatabaseEngine())) {
            for (IncludedDatabaseDriver includedDatabaseDriver : changesetEvaluator.getDrivers()) {
                if (includedDatabaseDriver.getDisplayName().equals(changesetEvaluator.getDatabaseEngine())) {
                    setProperty(properties, LiquibaseProperty.DRIVER, includedDatabaseDriver.getDriverClassName());
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("using db driver class[" + includedDatabaseDriver.getDriverClassName() + "] ");
                    }
                    break;
                }
            }
        }
    }

}
