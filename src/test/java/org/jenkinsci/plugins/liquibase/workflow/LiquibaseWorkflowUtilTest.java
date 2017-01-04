package org.jenkinsci.plugins.liquibase.workflow;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class LiquibaseWorkflowUtilTest {

    @Test
    public void should_construct_proper_changelog_param_string() {
        List<String> paramList = new ArrayList<String>();
        String colorKeyValue = "color=red";
        paramList.add(colorKeyValue);
        String shapeKeyValue = "shape=circle";
        paramList.add(shapeKeyValue);
        StepStub stepStub = new StepStub("");
        stepStub.setChangeLogParameterList(paramList);

        String nameKeyValue = "name=keith";
        String foodKeyValue = "food=hamburger";
        stepStub.setChangeLogParameters(nameKeyValue + "\n" + foodKeyValue);
        String result = LiquibaseWorkflowUtil.composeParameters(stepStub);

        assertThat(result, containsString(nameKeyValue));
        assertThat(result, containsString(shapeKeyValue));
        assertThat(result, containsString(foodKeyValue));
        assertThat(result, containsString(colorKeyValue));
    }

    class StepStub extends AbstractLiquibaseStep {
        public StepStub(String changeLogFile) {
            super(changeLogFile);
        }
    }
}