package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.EnvVars;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

import static org.exparity.hamcrest.date.IsSameInstant.sameInstant;
import static org.junit.Assert.assertThat;

public class RollbackBuilderTest {

    private static final String RESOLVE_FROM_DATE = "13/10/1973 8:00";
    private static final String HOURS_IN_DAY = "24";
    protected SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Test
    public void should_resolve_date_to_one_day_ago() throws ParseException {
        RollbackBuilder rollbackBuildStep = new RollbackBuilder();
        rollbackBuildStep.setRollbackLastHours(HOURS_IN_DAY);
        Date resolveFrom = sdf.parse(RESOLVE_FROM_DATE);
        Date result = rollbackBuildStep.resolveTargetDate(RollbackBuilder.RollbackStrategy.RELATIVE,resolveFrom, new EnvVars());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(resolveFrom);
        calendar.add(Calendar.HOUR, -24);
        Date theDayBefore = calendar.getTime();

        assertThat(result, sameInstant(theDayBefore));
    }

}