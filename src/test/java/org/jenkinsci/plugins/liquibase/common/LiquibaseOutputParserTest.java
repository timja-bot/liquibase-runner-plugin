package org.jenkinsci.plugins.liquibase.common;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jenkinsci.plugins.liquibase.evaluator.ChangeSetDetail;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.jenkinsci.plugins.liquibase.matchers.IsChangeSetDetail.isChangeSetDetail;
import static org.junit.Assert.assertThat;

public class LiquibaseOutputParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseOutputParserTest.class);


    protected LiquibaseOutputParser parser = new LiquibaseOutputParser();

    @Before
    public void setup() {

    }

    @Test
    public void should_find_all_changesets() throws IOException {
        String output = IOUtils.toString(getClass().getResourceAsStream("/parsing/stdout.txt"));

        List<ChangeSetDetail> changeSetDetails = parser.collectChangesets(output);
        assertThat(changeSetDetails, hasSize(4));

        assertThat(changeSetDetails, hasItems(
                isChangeSetDetail(
                        new ChangeSetDetail.Builder().withAuthor("keith").withId("create-table")
                                                     .withSuccessfullyExecuted(true)
                                                     .build())
                , isChangeSetDetail(new ChangeSetDetail.Builder().withAuthor("keith").withId("first_tag").build())));

    }

    @Test
    public void shouldExtractInfo() {
        ChangeSetDetail changeSetDetail = parser.extractInfo(
                "SEVERE 1/17/17 4:11 PM: liquibase: example-changesets/changeset-with-error.xml::sql-error::keith: Syntax error in SQL statement \"i[*]'m certainly not valid sql\"; SQL statement:");


        assertThat(changeSetDetail, is(isChangeSetDetail(
                new ChangeSetDetail.Builder().withPath("example-changesets/changeset-with-error.xml")
                                             .withId("sql-error").withAuthor("keith").build())));


    }


    @Test
    public void should_not_extract_info() {
        ChangeSetDetail changeSetDetail = parser.extractInfo(RandomStringUtils.randomAlphabetic(20));
        assertThat(changeSetDetail, nullValue());

    }

    @Test
    public void should_parse_error_line_successfully() {
        String line =
                "liquibase.exception.MigrationFailedException: Migration failed for change set example-changesets/changeset-with-error.xml::sql-error::keith:";

        ChangeSetDetail changeSetDetail = parser.extractInfo(line);

        assertThat(changeSetDetail, isChangeSetDetail(new ChangeSetDetail.Builder().withId("sql-error").withAuthor("keith").withPath("example-changesets/changeset-with-error.xml").build()));

    }

    @Test
    public void should_parse_error_output_correctly() throws IOException {
        String output = IOUtils.toString(getClass().getResourceAsStream("/parsing/error.output"));
        List<ChangeSetDetail> changeSetDetails = parser.collectChangesets(output);

        for (ChangeSetDetail changeSetDetail : changeSetDetails) {
            LOG.debug("changeSetDetail:{}", changeSetDetail);
        }
        assertThat(changeSetDetails, hasSize(2));
        assertThat(changeSetDetails.get(1).isSuccessfullyExecuted(), is(false));


    }

}