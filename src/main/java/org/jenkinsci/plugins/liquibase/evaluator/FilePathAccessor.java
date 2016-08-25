package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.FilePath;
import liquibase.resource.ResourceAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.filefilter.FileFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Provides Jenkin's file abstraction as a liquibase resource accessor.
 */
public class FilePathAccessor implements ResourceAccessor {
    private final FilePath filePath;

    private static final Logger LOG = LoggerFactory.getLogger(FilePathAccessor.class);

    public FilePathAccessor(FilePath filePath) {
        this.filePath = filePath;
    }

    public InputStream getResourceAsStream(String s) throws IOException {
        InputStream inputStream = null;

        if (filePath != null) {
            FilePath child = filePath.child(s);
            try {
                if (child.exists()) {
                    inputStream = child.read();
                }
            } catch (InterruptedException e) {
                throw new IOException("Error reading resource[" + s + "] ", e);
            }
        }

        return inputStream;
    }

    public Set<InputStream> getResourcesAsStream(String path) throws IOException {
        Set<InputStream> streams = null;
        try {
            InputStream resourceAsStream = getResourceAsStream(path);
            if (resourceAsStream!=null) {
                streams = Sets.newHashSet();
                streams.add(resourceAsStream);
            }
        } catch (IOException e) {
            LOG.info("Unable to load resources from path '" + path +"'", e);
        }
        return streams;
    }

    public Set<String> list(String relativeTo,
                            String path,
                            boolean includeFiles,
                            boolean includeDirectories,
                            boolean recursive) throws IOException {

        return list(filePath, relativeTo, path, includeFiles, includeDirectories, recursive);
    }

    @SuppressWarnings("ReturnOfNull")
    protected Set<String> list(FilePath workspace,
                               String relativeTo,
                               String path,
                               boolean includeFiles,
                               boolean includeDirectories, boolean recursive) throws IOException {
        Set<String> result = Sets.newHashSet();

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
                            result.addAll(list(workspace, relativeTo, path, includeFiles, includeDirectories, true));
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
                        throw new RuntimeException("Unable to construct classloader.",e);
                    }
                }
                return urlClassLoader;
            }
        });

        return urlClassLoader;
    }
}
