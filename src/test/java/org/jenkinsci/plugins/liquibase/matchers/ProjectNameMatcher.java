package org.jenkinsci.plugins.liquibase.matchers;

import hudson.model.AbstractProject;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ProjectNameMatcher extends TypeSafeMatcher<AbstractProject> {
    private final String jobName;

    public ProjectNameMatcher(String jobName) {
        this.jobName = jobName;
    }

    public static ProjectNameMatcher isProjectWithName(String expectedName) {
        return new ProjectNameMatcher(expectedName);
    }
    @Override
    protected boolean matchesSafely(AbstractProject item) {
        return item.getDisplayName().equals(jobName);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a project with display name of ").appendValue(jobName);
    }

    @Override
    protected void describeMismatchSafely(AbstractProject item, Description mismatchDescription) {
        mismatchDescription.appendText("a project with display name of ").appendValue(item.getDisplayName());
    }
}
