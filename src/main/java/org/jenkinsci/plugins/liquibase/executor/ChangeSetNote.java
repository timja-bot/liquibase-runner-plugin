package org.jenkinsci.plugins.liquibase.executor;

import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * finds changeset names and marks console accordingly.
 */
public class ChangeSetNote extends ConsoleNote {
    public static Pattern CHANGESET_PATTERN = Pattern.compile("(\\S+::)?(\\S+)::(\\S+): ");

    @Override
    public ConsoleAnnotator annotate(Object context, MarkupText text, int charPos) {
        Matcher matcher = CHANGESET_PATTERN.matcher(text.getText());
        while (matcher.find()) {
            String changeSetName = matcher.group(2);
            int start = matcher.start(2);
            text.addMarkup(start, changeSetName.length() + start, "<b class=liquibase-changeset>", "</b>");
        }

        return null;
    }

    public static boolean doesLineHaveChangeset(String line) {
        Matcher matcher = CHANGESET_PATTERN.matcher(line);
        return matcher.find();
    }


}
