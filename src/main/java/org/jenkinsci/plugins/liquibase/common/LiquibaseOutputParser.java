package org.jenkinsci.plugins.liquibase.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

/**
 * Known limitation:  fails when paths or IDs have spaces.
 */
public class LiquibaseOutputParser {

    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseOutputParser.class);

    public List<ChangeSetDetail> collectChangesets(CharSequence output) {
        List<ChangeSetDetail> changeSetDetails = new ArrayList<ChangeSetDetail>();
        Iterable<String> lines = Splitter.on("\n").trimResults().split(output);

        ChangeSetDetail last = null;
        for (String line : lines) {
            // sunny-day-changeset.xml: sunny-day-changeset.xml::create-testing-table4::keith: Table fortesting created
            // sunny-day-changeset.xml: sunny-day-changeset.xml::create-testing-table::keith: ChangeSet sunny-day-changeset.xml::create-testing-table::keith ran successfully in 5ms

            ChangeSetDetail detail = extractInfo(line);

            if (detail != null) {
                if (!detail.equals(last)) {
                    LOG.debug("adding {}", detail);
                    changeSetDetails.add(detail);
                } else {
                    if (!detail.isSuccessfullyExecuted() && last.isSuccessfullyExecuted()) {
                        changeSetDetails.remove(changeSetDetails.size() - 1);
                        changeSetDetails.add(detail);
                    }

                }
                last = detail;

            }


        }
        return changeSetDetails;
    }

    public ChangeSetDetail extractInfo(String line) {
        LOG.debug("parsing line:{}",line);
        ChangeSetDetail changeSetDetail = null;
        if (line.contains("::")) {
            String partial = line.substring(0, line.indexOf("::") + 2);
            String extracted = line.substring(partial.lastIndexOf(" ") + 1);
            Pattern pattern = Pattern.compile("([^:]*)::([^:]*)::([^:]*)");

            Matcher matcher = pattern.matcher(extracted);
            if (matcher.find()) {
                changeSetDetail = new ChangeSetDetail();
                changeSetDetail.setPath(matcher.group(1));

                if (matcher.groupCount()>=2) {
                    changeSetDetail.setId(matcher.group(2));
                }

                if (matcher.groupCount() >= 3) {
                    changeSetDetail.setAuthor(matcher.group(3));
                }
                if (line.contains(changeSetDetail.getAuthor() + " failed")) {
                    changeSetDetail.setSuccessfullyExecuted(false);

                }
                int groupCount = matcher.groupCount();

                LOG.debug("group count:{}", groupCount);
                for(int i = 0; i < groupCount+1; i++) {
                    LOG.debug("group {} is {}", i, matcher.group(i));
                }
                LOG.debug(matcher.group(3));

            } else {
                LOG.debug("no match");
            }
        }
        return changeSetDetail;
    }


}
