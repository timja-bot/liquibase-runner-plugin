package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import liquibase.resource.ResourceAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.filefilter.FileFileFilter;

import com.google.common.collect.Sets;

/**
 * Provides Jenkin's file abstraction as a liquibase resource accessor.
 */
public class FilePathAccessor implements ResourceAccessor {
    private final AbstractBuild<?, ?> build;

    public FilePathAccessor(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public InputStream getResourceAsStream(String s) throws IOException {
        FilePath child = build.getWorkspace().child(s);
        InputStream inputStream = null;
        try {
            if (child.exists()) {
                inputStream = child.read();
            }
        } catch (InterruptedException e) {
            throw new IOException("Error reading resource[" + s + "] ", e);
        }

        return inputStream;
    }

    public Set<InputStream> getResourcesAsStream(String path) throws IOException {
        Set<InputStream> streams = Sets.newHashSet();
        streams.add(getResourceAsStream(path));
        return streams;
    }

    public Set<String> list(String relativeTo,
                            String path,
                            boolean includeFiles,
                            boolean includeDirectories,
                            boolean recursive) throws IOException {

        Set<String> result = Sets.newHashSet();

        FilePath child;
        if (relativeTo == null) {
            child = build.getWorkspace().child(path);
        } else {
            child = build.getWorkspace().child(relativeTo).child(path);
        }

        try {
            if (child.isDirectory()) {
                if (includeDirectories) {
                    result.add(child.toURI().toURL().toString());
                }
                if (recursive) {
                    List<FilePath> dirs = child.listDirectories();
                    for (FilePath dir : dirs) {
                        result.addAll(list(null, dir.getRemote(), includeFiles, true, true));
                    }
                }
                if (includeFiles) {
                    List<FilePath> files = child.list(FileFileFilter.FILE);
                    for (FilePath filePath : files) {
                        result.add(filePath.toURI().toURL().toString());
                    }
                }
            }

            if (!child.isDirectory() && includeFiles) {
                result.add(child.getName());
            }
        } catch (InterruptedException e) {
            throw new IOException("Error getting resource '" + path + ".", e);
        }

        return result;
    }

    public ClassLoader toClassLoader() {
        try {
            return new URLClassLoader(new URL[]{new URL("file://" + build.getWorkspace().getBaseName())});
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
