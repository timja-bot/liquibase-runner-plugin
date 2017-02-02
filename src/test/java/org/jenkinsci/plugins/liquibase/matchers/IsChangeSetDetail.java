package org.jenkinsci.plugins.liquibase.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;

import static org.hamcrest.CoreMatchers.allOf;

public class IsChangeSetDetail {

    public static Matcher<ChangeSetDetail> hasId(final String id) {
        return new TypeSafeMatcher<ChangeSetDetail>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with id ").appendValue(id);
            }

            @Override
            protected void describeMismatchSafely(ChangeSetDetail item, Description mismatchDescription) {
                mismatchDescription.appendText("was id ").appendValue(item.getId());
            }

            @Override
            protected boolean matchesSafely(ChangeSetDetail changeSetDetail) {
                return id.equals(changeSetDetail.getId());
            }
        };
    }
    public static Matcher<ChangeSetDetail> isChangeSetDetail(ChangeSetDetail changeSetDetail) {
        return allOf(
                hasId(changeSetDetail.getId()), hasAuthor(changeSetDetail.getAuthor()), hasComments(changeSetDetail.getComments()));
    }

    public static Matcher<ChangeSetDetail> hasComments(final String comments) {
        return new TypeSafeMatcher<ChangeSetDetail>() {
            @Override
            protected boolean matchesSafely(ChangeSetDetail changeSetDetail) {
                boolean isMatch = true;
                if (comments == null) {
                    if (changeSetDetail.getComments() != null) {
                        isMatch = false;
                    }
                } else {
                    isMatch = comments.equals(changeSetDetail.getComments());
                }
                return isMatch;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with comments ").appendValue(comments);
            }

            @Override
            protected void describeMismatchSafely(ChangeSetDetail item, Description mismatchDescription) {
                mismatchDescription.appendText("was comments ").appendValue(item.getComments());
            }
        };
    }
    public static Matcher<ChangeSetDetail> hasAuthor(final String author) {
        return new TypeSafeMatcher<ChangeSetDetail>() {
            @Override
            protected boolean matchesSafely(ChangeSetDetail changeSetDetail) {
                return author.equals(changeSetDetail.getAuthor());
            }

            @Override
            protected void describeMismatchSafely(ChangeSetDetail item, Description mismatchDescription) {
                mismatchDescription.appendText("was author '").appendText(item.getAuthor()).appendText("'");
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("author '").appendText(author).appendText("'");
            }
        };
    }

}
