package org.jenkinsci.plugins.liquibase.matchers;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class BuildResultMatcher extends TypeSafeMatcher<FreeStyleBuild> {

    private Result expectedResult;


    public BuildResultMatcher(Result expectedResult) {
        this.expectedResult = expectedResult;

    }

    public static BuildResultMatcher isSuccessfulBuild() {
        return new BuildResultMatcher(Result.SUCCESS);
    }



    @Override
    protected boolean matchesSafely(FreeStyleBuild item) {
        return item.getResult().equals(expectedResult);
    }

    @Override
    protected void describeMismatchSafely(FreeStyleBuild item, Description mismatchDescription) {
        mismatchDescription.appendText("build whose result was ").appendText(item.getResult().toString());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("build whose result is ").appendText(expectedResult.toString());
    }

}
