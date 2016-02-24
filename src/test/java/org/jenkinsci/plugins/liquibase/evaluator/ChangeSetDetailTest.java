package org.jenkinsci.plugins.liquibase.evaluator;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class ChangeSetDetailTest {
    private static final Logger LOG = LoggerFactory.getLogger(ChangeSetDetailTest.class);

    @Test
    public void testTruncate() {
        StringBuilder longString = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for(int i = 0; i < ChangeSetDetail.MAX_LINES+1; i++) {
            String random = RandomStringUtils.randomAlphabetic(5) + "\n";
            longString.append(random);
            if(i< ChangeSetDetail.MAX_LINES) {
                expected.append(random);
            }
        }

        String result = ChangeSetDetail.truncateString(longString.toString());

        LOG.debug("result:{}", result);

        if (LOG.isDebugEnabled()) {
            LOG.debug("result[" + result + "] ");
        }
        assertEquals(expected.substring(0, expected.length() - 1), result);
    }

}