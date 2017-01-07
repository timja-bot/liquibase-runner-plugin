package org.jenkinsci.plugins.liquibase.common;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Run;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
     * Furthermore, any token expressions found are replaced with values found in the passed environment IF build
     * is an AbstractBuild.
     *
     * @param liquibaseBuilder
     * @param build
     * @param environment
     * @param workspace
     * @return
     */
    public static Properties createLiquibaseProperties(AbstractLiquibaseBuilder liquibaseBuilder,
                                                       Run<?, ?> build, Map environment, FilePath workspace)
            throws IOException, InterruptedException {
        Properties properties = new Properties();

        assembleDefaults(properties);
        assembleFromPropertiesFile(liquibaseBuilder, build, environment, workspace, properties);
        assembleFromProjectConfiguration(liquibaseBuilder, properties, environment, build);
        return properties;
    }

    private static void assembleFromPropertiesFile(AbstractLiquibaseBuilder liquibaseBuilder,
                                                   Run<?, ?> build,
                                                   Map environment, FilePath workspace, Properties properties) {
        String propertiesPath;
        if (build instanceof AbstractBuild) {
            propertiesPath = hudson.Util.replaceMacro(liquibaseBuilder.getLiquibasePropertiesPath(), environment);
        } else {
            propertiesPath = liquibaseBuilder.getLiquibasePropertiesPath();
        }
        assembleFromPropertiesFile(properties, propertiesPath, workspace);
    }

    protected static void assembleFromProjectConfiguration(AbstractLiquibaseBuilder liquibaseBuilder,
                                                           Properties properties,
                                                           Map environment, Run<?, ?> build)
            throws IOException, InterruptedException {


        if (!Strings.isNullOrEmpty(liquibaseBuilder.getCredentialsId())) {
            StandardUsernamePasswordCredentials credentials =
                    CredentialsProvider.findCredentialById(liquibaseBuilder.getCredentialsId(),
                            StandardUsernamePasswordCredentials.class, build,
                            Lists.<DomainRequirement>newArrayList());

            if (credentials!=null) {
                String username = credentials.getUsername();
                if (!Strings.isNullOrEmpty(username)) {
                    setProperty(properties, LiquibaseProperty.USERNAME, username);
                }
                setProperty(properties, LiquibaseProperty.PASSWORD, credentials.getPassword().getPlainText());
            }
        }
        addPropertyIfDefined(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile(),
                environment, build);

        addPropertyIfDefined(properties, LiquibaseProperty.CLASSPATH, liquibaseBuilder.getClasspath(), environment,
                build);
        addPropertyIfDefined(properties, LiquibaseProperty.DEFAULT_SCHEMA_NAME, liquibaseBuilder.getDefaultSchemaName(),
                environment, build);
        addPropertyIfDefined(properties, LiquibaseProperty.URL, liquibaseBuilder.getUrl(), environment, build);
        addPropertyIfDefined(properties, LiquibaseProperty.CHANGELOG_FILE, liquibaseBuilder.getChangeLogFile(),
                environment, build);
        addPropertyIfDefined(properties, LiquibaseProperty.LABELS, liquibaseBuilder.getLabels(), environment, build);
        addPropertyIfDefined(properties, LiquibaseProperty.CONTEXTS, liquibaseBuilder.getContexts(), environment, build);
        resolveDatabaseDriver(liquibaseBuilder, properties, environment, build);
    }

    private static void resolveDatabaseDriver(AbstractLiquibaseBuilder liquibaseBuilder,
                                              Properties properties,
                                              Map environment, Run<?, ?> build) {


        boolean useIncludedDriver = isBuilderUsingIncludedDriver(liquibaseBuilder);
        if (useIncludedDriver) {
            setDriverFromDBEngine(liquibaseBuilder, properties);
        } else {
            addPropertyIfDefined(properties, LiquibaseProperty.DRIVER, liquibaseBuilder.getDriverClassname(),
                    environment, build);
        }
    }

    private static boolean isBuilderUsingIncludedDriver(AbstractLiquibaseBuilder liquibaseBuilder) {
        boolean useIncludedDriver =
                liquibaseBuilder.hasUseIncludedDriverBeenSet() && liquibaseBuilder.isUseIncludedDriver();
        return useIncludedDriver && !Strings.isNullOrEmpty(liquibaseBuilder.getDatabaseEngine());
    }


    private static void assembleFromPropertiesFile(Properties properties,
                                                   String liquibasePropertiesPath,
                                                   FilePath workspace) {

        if (!Strings.isNullOrEmpty(liquibasePropertiesPath)) {
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

    private static void assembleDefaults(Properties properties) {
        setProperty(properties, LiquibaseProperty.DRIVER, DEFAULT_DB_DRIVER);
        setProperty(properties, LiquibaseProperty.URL, DEFAULT_JDBC_URL);
    }

    private static void setProperty(Properties properties, LiquibaseProperty liquibaseProperty, String value) {
        properties.setProperty(liquibaseProperty.propertyName(), value);
    }

    protected static void addPropertyIfDefined(Properties properties,
                                               LiquibaseProperty liquibaseProperty,
                                               String value, Map environment, Run<?, ?> build) {
        if (!Strings.isNullOrEmpty(value)) {
            String resolvedValue;
            if (build instanceof AbstractBuild) {
                resolvedValue = hudson.Util.replaceMacro(value, environment);
            } else {
                resolvedValue = value;
            }
            properties.setProperty(liquibaseProperty.propertyName(), resolvedValue);
        }
    }
}
