package org.jenkinsci.plugins.liquibase.common;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import hudson.FilePath;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.util.StringUtils;
import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        Driver driver;
        try {
            Driver actualDriver = (Driver) Class.forName(driverName, true, liquibaseClassLoader).getDeclaredConstructor().newInstance();
            driver = new DriverShim(actualDriver);

            DriverManager.registerDriver(driver);
        } catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot create driver " + driverName + ": " + e.getMessage(), e);
        }
    }

    public static ClassLoader createClassLoader(boolean isUnix, final FilePath workspace, String classpath) {
        String separator;
        if (isUnix) {
            separator = ":";
        } else {
            separator = ";";
        }

        List<URL> classpathUrls = new ArrayList<>();
        Iterable<String> classPathElements = Splitter.on(separator).trimResults().split(classpath);

        for (String filePath : classPathElements) {
            filePath = StringUtils.trimToNull(filePath);
            if (filePath == null) {
                continue;
            }

            //jenkins FilePath prefers unix style, even on windows
            filePath = filePath.replace("\\", "/");

            FilePath file = workspace.child(filePath);
            try {

                if (file.isDirectory()) {
                    for (FilePath jarFile : file.list("*.jar")) {
                        classpathUrls.add(jarFile.toURI().toURL());
                    }
                }
                classpathUrls.add(file.toURI().toURL());
            } catch (Exception e) {
                LOG.warn("Error handling classpath " + file+": "+e.getMessage(), e);
            }
        }

        return new URLClassLoader(Iterables.toArray(classpathUrls, URL.class), Util.class.getClassLoader());
    }
}
