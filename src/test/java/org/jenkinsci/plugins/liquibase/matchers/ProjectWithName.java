package org.jenkinsci.plugins.liquibase.matchers;

import hudson.model.AbstractProject;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class ProjectWithName extends TypeSafeMatcher<AbstractProject> {
    private final String jobName;

    public ProjectWithName(String jobName) {
        this.jobName = jobName;
    }

    public static ProjectWithName isProjectWithName(String expectedName) {
        return new ProjectWithName(expectedName);
    }
    @Override
    protected boolean matchesSafely(AbstractProject item) {
        return item.getDisplayName().equals(jobName);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a project with display name of ").appendValue(jobName);

    }
}
