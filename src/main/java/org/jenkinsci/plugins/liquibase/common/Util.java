package org.jenkinsci.plugins.liquibase.common;

import hudson.FilePath;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.annotation.Nullable;

import org.jenkinsci.plugins.liquibase.exception.LiquibaseRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

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

    public static void registerDatabaseDriver(String driverName, String classpath)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
        Driver driver;
        if (!Strings.isNullOrEmpty(classpath)) {
            Driver actualDriver =
                    (Driver) Class.forName(driverName, true, Thread.currentThread().getContextClassLoader())
                                  .newInstance();
            driver = new DriverShim(actualDriver);
        } else {
            driver = (Driver) Class.forName(driverName).newInstance();
        }

        DriverManager.registerDriver(driver);
    }

    public static void addClassloader(boolean isUnix, final FilePath workspace, String classpath) {
        String separator;
        if (isUnix) {
            separator = ":";
        } else {
            separator = ";";
        }
        Iterable<String> classPathElements = Splitter.on(separator).trimResults().split(classpath);
        final Iterable<URL> urlIterable = Iterables.transform(classPathElements, new Function<String, URL>() {
            @Override
            public URL apply(@Nullable String filePath) {
                URL url = null;
                if (filePath != null) {
                    try {
                        if (Paths.get(filePath).isAbsolute()) {
                            url = new File(filePath).toURI().toURL();
                        } else {
                            URI workspaceUri = workspace.toURI();
                            File workspace = new File(workspaceUri);
                            url = new File(workspace, filePath).toURI().toURL();
                        }
                    } catch (MalformedURLException e) {
                        LOG.warn("Unable to transform classpath element " + filePath, e);
                    } catch (InterruptedException e) {
                        throw new LiquibaseRuntimeException("Error during database driver resolution", e);
                    } catch (IOException e) {
                        throw new LiquibaseRuntimeException("Error during database driver resolution", e);
                    }
                }
                return url;
            }
        });

        URLClassLoader urlClassLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                return new URLClassLoader(Iterables.toArray(urlIterable, URL.class),
                        Thread.currentThread().getContextClassLoader());
            }
        });
        Thread.currentThread().setContextClassLoader(urlClassLoader);

    }
}
