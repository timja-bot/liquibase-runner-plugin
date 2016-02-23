package org.jenkinsci.plugins.liquibase.external;

import hudson.MarkupText;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ChangeSetNoteTest {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeSetNoteTest.class);
    protected ChangeSetNote note;

    @Before
    public void setup() {
        note = new ChangeSetNote();
    }
    @Test
    public void should_mark_executed_changeset() {
        MarkupTextStub text =
                new MarkupTextStub( "liquibase: changeset.xml: /some/path::create-table::keith: Table shape created");
        note.annotate(null, text, 0);

        assertThat(text.wasAddMarkupCalled(), is(true));
    }

    @Test
    public void should_not_mark_nonmatch() {
        MarkupTextStub nonmatchingText = new MarkupTextStub("I'm not text that indicates a changeset was executedd");
        note.annotate(null, nonmatchingText, 0);
        assertThat(nonmatchingText.wasAddMarkupCalled(), is(false));
    }



    class MarkupTextStub extends MarkupText {

        boolean addMarkupWasCalled = false;
        public MarkupTextStub(String text) {
            super(text);
        }

        @Override
        public void addMarkup(int startPos, int endPos, String startTag, String endTag) {
            super.addMarkup(startPos, endPos, startTag, endTag);
            addMarkupWasCalled=true;
        }

        public boolean wasAddMarkupCalled() {
            return addMarkupWasCalled;
        }
    }

}