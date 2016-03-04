package org.jenkinsci.plugins.liquibase.common;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;

public class Util {

    private static final String ERROR_STRING = "Errors:";

    public static final String UNEXPECTED_ERROR = "Unexpected error running Liquibase";
    private Util() {
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
