package org.jenkinsci.plugins.liquibase.common;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Util {
    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

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

    public static void registerDatabaseDriver(String driverName, ClassLoader liquibaseClassLoader) throws SQLException {
        try {
            Driver driver = (Driver) Class.forName(driverName, true, liquibaseClassLoader).getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(driver);
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot create driver " + driverName + ": " + e.getMessage(), e);
        }
    }
}
