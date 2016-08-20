package org.jenkinsci.plugins.liquibase.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.rules.TemporaryFolder;

public class LiquibaseTestUtil {
    static final String SUNNY_DAY_CHANGESET_XML = "/example-changesets/sunny-day-changeset.xml";
    private static final String CHANGESET_WITH_ERROR_XML = "/example-changesets/changeset-with-error.xml";
    static final String IN_MEMORY_JDBC_URL = "jdbc:h2:mem:test";

    static File createErrorFreeChangeset(TemporaryFolder temporaryFolder) throws IOException {
        return createFileFromResource(temporaryFolder.getRoot(), SUNNY_DAY_CHANGESET_XML);
    }

    static File createChangesetFileWithError(TemporaryFolder temporaryFolder) throws IOException {
        return createFileFromResource(temporaryFolder.getRoot(), CHANGESET_WITH_ERROR_XML);
    }

    static void createProjectFiles(TemporaryFolder temporaryFolder, String... resourcePaths) throws IOException {
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
}
