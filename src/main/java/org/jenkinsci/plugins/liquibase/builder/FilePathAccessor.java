package org.jenkinsci.plugins.liquibase.builder;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import liquibase.resource.ResourceAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

class FilePathAccessor implements ResourceAccessor {
    private final AbstractBuild<?, ?> build;

    public FilePathAccessor(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public InputStream getResourceAsStream(String s) throws IOException {
        FilePath child = build.getWorkspace().child(s);
        InputStream inputStream=null;
        try {
            if (child.exists()) {
                inputStream = child.read();
            }
        } catch (InterruptedException e) {
            throw new IOException("Error reading resource[" + s + "] ", e);
        }

        return inputStream;
    }

    public Enumeration<URL> getResources(String s) throws IOException {
        Enumeration<URL> o = null;
        FilePath childDir = build.getWorkspace().child(s);
        try {
            List<URL> urls= new ArrayList<URL>();
            if (childDir.isDirectory()) {
                List<FilePath> children = childDir.list();
                for (FilePath child : children) {
                    urls.add(child.toURI().toURL());

                }
                o= Collections.enumeration(urls);

            } else {
                urls.add(childDir.toURI().toURL());

            }
        } catch (InterruptedException e) {
            throw new IOException("Error loading resources from[" + s + "] ", e);
        }


        return o;
    }

    public ClassLoader toClassLoader() {
        try {
            return new URLClassLoader(new URL[]{new URL("file://" + build.getWorkspace().getBaseName())});
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
