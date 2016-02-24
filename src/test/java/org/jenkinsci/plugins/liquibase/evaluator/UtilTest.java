package org.jenkinsci.plugins.liquibase.evaluator;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.plugins.liquibase.common.Util;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UtilTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private File withError;
    private File withoutError;

    @Before
    public void setup() throws IOException {
        withError = testFolder.newFile();
        Files.write("Errors:\n" + "  Command not passed", withError, Charsets.UTF_8);

        withoutError = testFolder.newFile();
        Files.write("I'm just a build log", withoutError, Charsets.UTF_8);
    }

    @Test
    public void should_detect_error() throws IOException {
        boolean doesErrorExist = Util.doesErrorExist(withError);
        assertThat(doesErrorExist, is(true));
    }

    @Test
    public void should_detect_no_errors() throws IOException {
        boolean errorExist = Util.doesErrorExist(withoutError);
        assertThat(errorExist, is(false));

    }
}