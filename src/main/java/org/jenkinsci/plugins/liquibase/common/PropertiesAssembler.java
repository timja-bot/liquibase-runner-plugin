package org.jenkinsci.plugins.liquibase.common;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.liquibase.evaluator.AbstractLiquibaseBuilder;
import org.jenkinsci.plugins.liquibase.evaluator.IncludedDatabaseDriver;
import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class PropertiesAssembler {
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesAssembler.class);
    private static final String DEFAULT_JDBC_URL = "jdbc:h2:mem:builder-db";
    private static final String DEFAULT_DB_DRIVER = "org.h2.Driver";

    /**
     * Creates a properties instance for use with liquibase execution.  Property values may come from these sources,
     * in order of least to most precedence:
     * <ul>
     * <li>Plugin Default values</li>
     * <li>Values from properties file described by {@link AbstractLiquibaseBuilder#liquibasePropertiesPath}</li>
     * <li>Values on the {@link AbstractLiquibaseBuilder} itself.</li>
     * </ul>
     * Furthermore, any token expressions found are replaced with values found in the passed environment.
     *
     * @param liquibaseBuilder
     * @param build
     * @param environment
     * @return
     */
    public static Properties createLiquibaseProperties(AbstractLiquibaseBuilder liquibaseBuilder,
                                                       AbstractBuild<?, ?> build, EnvVars environment)
            throws IOException, InterruptedException {
        Properties properties = new Properties();
        assembleDefaults(properties);
        String propertiesPath = resolvePropertiesPath(liquibaseBuilder, environment);
        assembleFromPropertiesFile(properties, propertiesPath, build);

        assembleFromProjectConfiguration(liquibaseBuilder, properties, environment, build);
        return properties;
    }

    protected static void assembleFromProjectConfiguration(AbstractLiquibaseBuilder liquibaseBuilder,
                                                           Properties properties,
                                                           EnvVars environment, AbstractBuild<?, ?> build)
            throws IOException, InterruptedException {


        if (!Strings.isNullOrEmpty(liquibaseBuilder.getCredentialsId())) {
            StandardUsernamePasswordCredentials credentialById =
                    CredentialsProvider.findCredentialById(liquibaseBuilder.getCredentialsId(),
                            StandardUsernamePasswordCredentials.class, build,
                            Lists.<DomainRequirement>newArrayList());

            if (credentialById!=null) {
                addPropertyIfDefined(properties, LiquibaseProperty.USERNAME, credentialById.getUsername(), environment);
                addPropertyIfDefined(properties, LiquibaseProperty.PASSWORD, credentialById.getPassword().getPlainText(), environment);
            }
        }



        addPropertyIfDefined(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile(),
                environment);

        addPropertyIfDefined(properties, LiquibaseProperty.CLASSPATH, liquibaseBuilder.getClasspath(), environment);
        addPropertyIfDefined(properties, LiquibaseProperty.DEFAULT_SCHEMA_NAME, liquibaseBuilder.getDefaultSchemaName(),
                environment);
        addPropertyIfDefined(properties, LiquibaseProperty.URL, liquibaseBuilder.getUrl(), environment);
        addPropertyIfDefined(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile(),
                environment);
        addPropertyIfDefined(properties, LiquibaseProperty.LABELS, liquibaseBuilder.getLabels(), environment);
        addPropertyIfDefined(properties, LiquibaseProperty.CONTEXTS, liquibaseBuilder.getContexts(), environment);
        resolveDatabaseDriver(liquibaseBuilder, properties, environment);
    }

    private static void resolveDatabaseDriver(AbstractLiquibaseBuilder liquibaseBuilder,
                                              Properties properties,
                                              EnvVars environment) {


        boolean useIncludedDriver = useIncludedDriver(liquibaseBuilder);
        if (useIncludedDriver) {
            PropertiesAssembler.setDriverFromDBEngine(liquibaseBuilder, properties);
        } else {
            addPropertyIfDefined(properties, LiquibaseProperty.DRIVER, liquibaseBuilder.getDriverClassname(),
                    environment);
        }
    }

    private static boolean useIncludedDriver(AbstractLiquibaseBuilder liquibaseBuilder) {
        boolean useIncludedDriver =
                liquibaseBuilder.hasUseIncludedDriverBeenSet() && liquibaseBuilder.isUseIncludedDriver();
        return useIncludedDriver && !Strings.isNullOrEmpty(liquibaseBuilder.getDatabaseEngine());
    }


    private static void assembleFromPropertiesFile(Properties properties,
                                                   String liquibasePropertiesPath,
                                                   AbstractBuild<?, ?> build) {
        if (!Strings.isNullOrEmpty(liquibasePropertiesPath)) {
            FilePath workspace = build.getWorkspace();
            if (workspace != null) {
                InputStreamReader streamReader = null;
                try {
                    FilePath liquibaseProperties = workspace.child(liquibasePropertiesPath);
                    streamReader = new InputStreamReader(liquibaseProperties.read(), StandardCharsets.UTF_8);
                    properties.load(streamReader);
                } catch (IOException e) {
                    throw new LiquibaseRuntimeException(
                            "Unable to load properties file at '" + liquibasePropertiesPath + "'", e);
                } catch (InterruptedException e) {
                    throw new LiquibaseRuntimeException(
                            "Unable to load properties file at '" + liquibasePropertiesPath + "'", e);

                } finally {
                    IOUtils.closeQuietly(streamReader);
                }
            } else {
                throw new LiquibaseRuntimeException(
                        "Project workspace was found to be null when attempting to load liquibase properties file at '" +
                                liquibasePropertiesPath + '.');
            }
        }
    }

    private static void assembleDefaults(Properties properties) {
        setProperty(properties, LiquibaseProperty.DRIVER, DEFAULT_DB_DRIVER);
        setProperty(properties, LiquibaseProperty.URL, DEFAULT_JDBC_URL);
    }

    private static void setProperty(Properties properties, LiquibaseProperty liquibaseProperty, String value) {
        properties.setProperty(liquibaseProperty.propertyName(), value);
    }

    protected static void addPropertyIfDefined(Properties properties,
                                               LiquibaseProperty liquibaseProperty,
                                               String value, EnvVars environment) {
        if (!Strings.isNullOrEmpty(value)) {
            String resolvedValue = hudson.Util.replaceMacro(value, environment);
            properties.setProperty(liquibaseProperty.propertyName(), resolvedValue);
        }
    }

    private static String resolvePropertiesPath(AbstractLiquibaseBuilder liquibaseBuilder, EnvVars environment) {
        return hudson.Util.replaceMacro(liquibaseBuilder.getLiquibasePropertiesPath(), environment);
    }

    public static void setDriverFromDBEngine(AbstractLiquibaseBuilder liquibaseBuilder, Properties properties) {
        if (!Strings.isNullOrEmpty(liquibaseBuilder.getDatabaseEngine())) {
            for (IncludedDatabaseDriver includedDatabaseDriver : liquibaseBuilder.getDrivers()) {
                if (includedDatabaseDriver.getDisplayName().equals(liquibaseBuilder.getDatabaseEngine())) {
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
