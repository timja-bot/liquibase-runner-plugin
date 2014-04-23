package org.jenkinsci.plugins.liquibase.builder;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class Util {
    private Util() {
    }

    protected static boolean doesErrorExist(File logFile) throws IOException {
        return Files.readLines(logFile, Charsets.UTF_8, new LineProcessor<Boolean>() {
            boolean containsError;

            public boolean processLine(String line) throws IOException {
                boolean continueProcessing = true;
                if (line.contains("Errors:")) {
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
}
