package org.jenkinsci.plugins.liquibase.builder;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChangeSetNoteTest {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeSetNoteTest.class);
    private final String lineWithMatch =
            "INFO 4/25/14 11:34 AM:liquibase: changeset.xml: sample::keith: ChangeSet changeset.xml::sample::keith ran successfully in 167ms";

    private String anotherLine="INFO 4/25/14 1:50 PM:liquibase: changeset.xml: data_insert.sql::column_add::keith: ChangeSet data_insert.sql::column_add::keith ran successfully in 188ms";

    @Test
    public void testPattern() {

        Matcher matcher = ChangeSetNote.CHANGESET_PATTERN.matcher(lineWithMatch);


        boolean matches = matcher.find();
        if (LOG.isDebugEnabled()) {
            LOG.debug("matches? " + matches);

        }

        assertThat(matches, is(true));


    }

    @Test
    public void testNoMatch() {
        String line = "KeithWasHere";
        Matcher matcher = ChangeSetNote.CHANGESET_PATTERN.matcher(line);
        assertThat(matcher.find(), is(false));

    }

    @Test
    public void testGroupedLines() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/output.txt");
        LineIterator lineIterator = IOUtils.lineIterator(resourceAsStream, "UTF-8");
        while (lineIterator.hasNext()) {
            printMatches(lineIterator.nextLine());
        }



    }

    @Test
    public void testGrouped() {
        String anotherLine1 = anotherLine;
        printMatches(anotherLine1);

    }

    private void printMatches(String anotherLine1) {
        Matcher matcher = ChangeSetNote.CHANGESET_PATTERN.matcher(anotherLine1);
        while (matcher.find()) {
            if(LOG.isDebugEnabled()) {
            	LOG.debug(matcher.group(3));

            }
        }
    }

}