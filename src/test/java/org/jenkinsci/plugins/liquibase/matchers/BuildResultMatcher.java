package org.jenkinsci.plugins.liquibase.matchers;

import hudson.model.Result;
import hudson.model.Run;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class BuildResultMatcher extends TypeSafeMatcher<Run> {

    private Result expectedResult;

    public BuildResultMatcher(Result expectedResult) {
        this.expectedResult = expectedResult;
    }

    public static BuildResultMatcher isSuccessful() {
        return new BuildResultMatcher(Result.SUCCESS);
    }

    public static BuildResultMatcher isUnstable() {
        return new BuildResultMatcher(Result.UNSTABLE);
    }
    public static BuildResultMatcher isFailure() {
        return new BuildResultMatcher(Result.FAILURE);
    }

    @Override
    protected boolean matchesSafely(Run item) {
        return item.getResult().equals(expectedResult);
    }

    @Override
    protected void describeMismatchSafely(Run item, Description mismatchDescription) {
        mismatchDescription.appendText("build whose result was ").appendValue(item.getResult().toString());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("build whose result is ").appendValue(expectedResult.toString());
    }

}
