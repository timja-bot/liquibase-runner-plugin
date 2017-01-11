package org.jenkinsci.plugins.liquibase.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.google.common.base.Strings;

public class InputCheckedMatcher extends TypeSafeMatcher<HtmlInput> {

    boolean checkedExpected = false;

    public InputCheckedMatcher(boolean checkedExpected) {
        this.checkedExpected = checkedExpected;
    }

    public static InputCheckedMatcher isChecked() {
        return new InputCheckedMatcher(true);
    }
    public static InputCheckedMatcher isNotChecked() {
        return new InputCheckedMatcher(false);
    }


    @Override
    protected boolean matchesSafely(HtmlInput item) {
        boolean isChecked = !Strings.isNullOrEmpty(item.getCheckedAttribute());

        boolean result = false;
        if (checkedExpected && isChecked) {
            result = true;
        }
        if (!checkedExpected && !isChecked) {
            result = true;
        }
        return result;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("html input is ");
        if (checkedExpected) {
            description.appendText("checked");
        } else {
            description.appendText("is not checked");
        }
    }
}
