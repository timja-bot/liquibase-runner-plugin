package org.jenkinsci.plugins.liquibase.integration;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;

public class IsChangesetDetailWithId extends TypeSafeMatcher<ChangeSetDetail> {
    ChangeSetDetail expected;


    public static IsChangesetDetailWithId changeSetDetailWithId(ChangeSetDetail changeSetDetail) {
        return new IsChangesetDetailWithId(changeSetDetail);
    }

    public static IsChangesetDetailWithId changeSetDetailWithId(String expectedId) {

        ChangeSetDetail expected = new ChangeSetDetail();
        expected.setId(expectedId);
        return new IsChangesetDetailWithId(expected);
    }

    public IsChangesetDetailWithId(ChangeSetDetail expected) {
        this.expected=expected;
    }

    @Override
    protected boolean matchesSafely(ChangeSetDetail changeSetDetail) {
        return expected.getId().equals(changeSetDetail.getId());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("changeSetDetailWithId(").appendText(this.expected.getId()).appendText(")");
    }

    @Override
    protected void describeMismatchSafely(ChangeSetDetail item, Description mismatchDescription) {
        mismatchDescription.appendText("was a changeSetDetailWithId ").appendText(item.getId());
    }
}
