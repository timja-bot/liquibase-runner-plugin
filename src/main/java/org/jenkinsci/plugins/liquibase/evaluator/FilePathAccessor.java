package org.jenkinsci.plugins.liquibase.evaluator;

import com.google.common.collect.Sets;
import hudson.FilePath;
import liquibase.resource.AbstractResourceAccessor;
import liquibase.resource.InputStreamList;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

/**
 * Provides Jenkin's file abstraction as a liquibase resource accessor.
 */
public class FilePathAccessor extends AbstractResourceAccessor {
    private final FilePath filePath;

    private static final Logger LOG = LoggerFactory.getLogger(FilePathAccessor.class);

    public FilePathAccessor(FilePath filePath) {
        this.filePath = filePath;
    }

    @Override
    public InputStreamList openStreams(String relativeTo, String streamPath) throws IOException {
        InputStreamList streams = new InputStreamList();

        if (filePath != null) {
            FilePath relativeToPath = null;
            if (relativeTo == null) {
                relativeToPath = filePath;
            } else {
                relativeToPath = filePath.child(relativeTo);
            }

            FilePath child = relativeToPath.child(streamPath);
            try {
                if (child.exists()) {
                    streams.add(child.toURI(), child.read());
                }
            } catch (InterruptedException e) {
                throw new IOException("Error reading resource[" + streamPath + "] ", e);
            }
        }

        return streams;
    }

    @Override
    public SortedSet<String> describeLocations() {
        try {
            return new TreeSet<>(Arrays.asList(filePath.toURI().toString()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SortedSet<String> list(String relativeTo, String path, boolean recursive, boolean includeFiles, boolean includeDirectories) throws IOException {
        return list(filePath, relativeTo, path, recursive, includeFiles, includeDirectories);
    }

    @SuppressWarnings("ReturnOfNull")
    protected SortedSet<String> list(FilePath workspace,
                               String relativeTo,
                               String path,
                               boolean recursive,
                               boolean includeFiles,
                               boolean includeDirectories) throws IOException {
        SortedSet<String> result = Sets.newTreeSet();

        if (workspace != null) {
            FilePath child;
            if (relativeTo == null) {
                child = workspace.child(path);
            } else {
                child = workspace.child(relativeTo).child(path);
            }

            try {
                List<FilePath> filePaths = child.list();

                for (int i = 0; i < filePaths.size(); i++) {
                    FilePath filePath = filePaths.get(i);
                    if (filePath.isDirectory()) {

                        if (includeDirectories) {
                            result.add(filePath.getRemote());
                        }
                        if (recursive) {
                            result.addAll(list(workspace, relativeTo, path, true, includeFiles, includeDirectories));
                        }
                    } else {
                        if (includeFiles) {
                            result.add(filePath.getRemote());
                        }
                    }
                }
                if (child.isDirectory()) {
                    if (includeDirectories) {
                        result.add(child.getRemote());
                    }
                    if (recursive) {
                        List<FilePath> dirs = child.listDirectories();
                        for (FilePath dir : dirs) {
                            result.addAll(list(null, dir.getRemote(), includeFiles, includeDirectories, true));
                        }
                    }
                } else {
                    if (includeFiles) {
                        List<FilePath> files = child.list(FileFileFilter.FILE);
                        for (FilePath filePath : files) {
                            result.add(filePath.getRemote());
                        }
                    }
                }

                if (!child.isDirectory() && includeFiles) {
                    result.add(child.getName());
                }
            } catch (InterruptedException e) {
                throw new IOException("Error getting resource '" + path + ".", e);
            }
        }

        if (result.isEmpty()) {
            return null;
        } else {
            return result;
        }
    }

    public ClassLoader toClassLoader() {
        URLClassLoader urlClassLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            @Override
            public URLClassLoader run() {
                URLClassLoader urlClassLoader = null;
                if (filePath != null) {
                    try {
                        urlClassLoader =
                                new URLClassLoader(new URL[]{new URL("file://" + filePath.getBaseName())});
                    } catch (MalformedURLException e) {
                        throw new RuntimeException("Unable to construct classloader.", e);
                    }
                }
                return urlClassLoader;
            }
        });

        return urlClassLoader;
    }
}
