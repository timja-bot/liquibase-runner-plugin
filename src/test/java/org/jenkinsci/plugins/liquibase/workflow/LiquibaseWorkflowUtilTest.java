package org.jenkinsci.plugins.liquibase.workflow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LiquibaseWorkflowUtilTest {

    @Test
    public void should_construct_proper_changelog_param_string() {
        List<String> paramList = new ArrayList<String>();
        paramList.add("color=red");
        paramList.add("shape=circle");
        String result = LiquibaseWorkflowUtil.composeParameters(paramList);

        String expected = "color=red\n" +
                "shape=circle";
        assertThat(result, is(expected));
    }
}