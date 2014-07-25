package org.jenkinsci.plugins.liquibase.common;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class Util {

    private static final String ERROR_STRING = "Errors:";

    private Util() {
    }

    /**
     * Simple routine to check for the string "Errors:" in a file.
     * @param logFile file to scan.
     * @return true if {@link #ERROR_STRING} appears in file.
     * @throws IOException
     */
    protected static boolean doesErrorExist(File logFile) throws IOException {
        return Files.readLines(logFile, Charsets.UTF_8, new LineProcessor<Boolean>() {
            boolean containsError;

            public boolean processLine(String line) throws IOException {
                boolean continueProcessing = true;
                if (line !=null && line.contains(ERROR_STRING)) {
                    containsError = true;
                    continueProcessing = false;
                }
                return continueProcessing;
            }

            public Boolean getResult() {
                return containsError;
            }
        });
    }

    public static String formatChangeset(ChangeSet changeSet) {
        String filePath;
        DatabaseChangeLog log = changeSet.getChangeLog();
        if (log != null) {
            filePath = log.getFilePath();
        } else {
            filePath = "";
        }
        String changeSetName = changeSet.toString(false);

        return filePath + "::" + changeSetName.replace(filePath + "::", "");
    }
}
