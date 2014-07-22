package org.jenkinsci.plugins.liquibase.builder;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ChangeSetDetailTest {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeSetDetailTest.class);


    @Test
    public void testTruncate() {
        StringBuilder stringBuilder = new StringBuilder();

        StringBuilder expected = new StringBuilder();
        for(int i = 0; i < 25; i++) {
            String random = RandomStringUtils.randomAlphabetic(5) + "\n";
            stringBuilder.append(random);
            if(i< ChangeSetDetail.MAX_LINES) {
                expected.append(random);
            }
        }

        String result = ChangeSetDetail.truncateString(stringBuilder.toString());

        if (LOG.isDebugEnabled()) {
            LOG.debug("result[" + result + "] ");
        }

        assertEquals(expected.substring(0, expected.length() - 1), result);




    }

}