package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleProject;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

public class LiquibaseTestUtil {
    static final String SUNNY_DAY_CHANGESET_XML = "/example-changesets/sunny-day-changeset.xml";
    private static final String CHANGESET_WITH_ERROR_XML = "/example-changesets/changeset-with-error.xml";
    public static final String IN_MEMORY_JDBC_URL = "jdbc:h2:mem:test";
    public static final String H2 = "H2";


    static File createErrorFreeChangeset(TemporaryFolder temporaryFolder) throws IOException {
        return createFileFromResource(temporaryFolder.getRoot(), SUNNY_DAY_CHANGESET_XML);
    }

    static File createChangesetFileWithError(TemporaryFolder temporaryFolder) throws IOException {
        return createFileFromResource(temporaryFolder.getRoot(), CHANGESET_WITH_ERROR_XML);
    }

    static void createFilesFromResources(TemporaryFolder temporaryFolder, String... resourcePaths) throws IOException {
        for (int i = 0; i < resourcePaths.length; i++) {
            String resourcePath = resourcePaths[i];
            createFileFromResource(temporaryFolder.getRoot(), resourcePath);
        }
    }

    public static File createFileFromResource(File parent, String sourceResourcePath) throws IOException {
        String filename = extractFilenameFromResourcePath(sourceResourcePath);
        File changesetFile = new File(parent, filename);
        InputStream resourceAsStream = LiquibaseTestUtil.class.getResourceAsStream(sourceResourcePath);
        String changeset = IOUtils.toString(resourceAsStream);
        FileUtils.write(changesetFile, changeset);
        return changesetFile;
    }

    static String extractFilenameFromResourcePath(String sourceResourcePath) {
        return sourceResourcePath.substring(sourceResourcePath.lastIndexOf("/")+1, sourceResourcePath.length());
    }

    public static FreeStyleProject createLiquibaseProject(File changelogFile, JenkinsRule jenkinsRule)
            throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        ChangesetEvaluator evaluator = new ChangesetEvaluator();
        evaluator.setChangeLogFile(changelogFile.getAbsolutePath());
        evaluator.setUrl(IN_MEMORY_JDBC_URL);
        evaluator.setDatabaseEngine(H2);
        project.getBuildersList().add(evaluator);
        return project;
    }

    public static void createDatabase(String dbUrl, File changeset) throws IOException, SQLException, LiquibaseException {
        InputStream asStream = LiquibaseTestUtil.class.getResourceAsStream("/example-changesets/unit-test.h2.liquibase.properties");
        Properties liquibaseProperties = new Properties();
        liquibaseProperties.load(asStream);

        Connection connection = DriverManager.getConnection(dbUrl, liquibaseProperties);
        JdbcConnection jdbcConnection = new JdbcConnection(connection);
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);

        Liquibase liquibase = new Liquibase(changeset.getAbsolutePath(), new FileSystemResourceAccessor(), database);
        liquibase.update(new Contexts());
    }

    public static String composeJdbcUrl(File databaseFile) {
        return "jdbc:h2:file:" + databaseFile.getAbsolutePath();
    }
}
