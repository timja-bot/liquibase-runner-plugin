package org.jenkinsci.plugins.liquibase.integration;

import hudson.model.FreeStyleProject;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.jenkinsci.plugins.liquibase.matchers.InputCheckedMatcher.isChecked;
import static org.jenkinsci.plugins.liquibase.matchers.InputCheckedMatcher.isNotChecked;
import static org.junit.Assert.assertThat;

public class DriverSelectionFormTest {

    private static final Logger LOG = LoggerFactory.getLogger(DriverSelectionFormTest.class);

    @ClassRule
    public static JenkinsRule jenkinsRule = new JenkinsRule();


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    protected JenkinsRule.WebClient webClient;
    protected FreeStyleProject project;
    protected ChangesetEvaluator evaluator;

    @Before
    public void setup() throws IOException, SAXException {

        webClient = jenkinsRule.createWebClient();
        project = jenkinsRule.createFreeStyleProject(RandomStringUtils.randomAlphabetic(8));
        evaluator = new ChangesetEvaluator();
        evaluator.setUrl(LiquibaseTestUtil.IN_MEMORY_JDBC_URL);
        project.getBuildersList().add(evaluator);
        evaluator.setChangeLogFile(LiquibaseTestUtil
                .createFileFromResource(temporaryFolder.getRoot(), "/example-changesets/single-changeset.xml")
                .getAbsolutePath());

    }

    @Test
    public void should_hide_driverclassname() throws IOException, SAXException {
        evaluator.setDatabaseEngine(LiquibaseTestUtil.H2);
        project.save();

        HtmlForm config = clickAdvanceConfig();

        List<?> radios = config.getByXPath("//*[@type='radio' and contains(@name,'Included')]");
        assertThat(radios, hasSize(2));
        HtmlInput useIncludedRadio = (HtmlInput) radios.get(0);
        HtmlInput useSpecifiedDriver = (HtmlInput) radios.get(1);

        List<?> inputs = config.getByXPath("//*[@name='_.driverClassname']");
        HtmlInput driverClassnameInput = (HtmlInput) inputs.get(0);

        LOG.debug("classname input:{}", driverClassnameInput);
        LOG.debug("disabled? {}", driverClassnameInput.isDisabled());

        LOG.debug("radios size:{}", radios.size());

        assertThat(useIncludedRadio, isChecked());
        assertThat(useSpecifiedDriver, isNotChecked());
        assertThat(isDriverClassnameRowHidden(config), is(true));

    }

    @Test
    public void should_have_specified_driverclassname_enabled() throws IOException, SAXException {
        String driverClassname = RandomStringUtils.randomAlphabetic(10);
        evaluator.setDriverClassname(driverClassname);
        evaluator.clearDatabaseEngine();
        project.save();

        HtmlForm config = clickAdvanceConfig();

        List<?> radios = config.getByXPath("//*[@type='radio' and contains(@name,'Included')]");

        assertThat(radios, hasSize(2));
        HtmlInput useIncludedRadio = (HtmlInput) radios.get(0);
        HtmlInput useSpecifiedDriver = (HtmlInput) radios.get(1);

        assertThat(useIncludedRadio, isNotChecked());
        assertThat(useSpecifiedDriver, isChecked());
        assertThat(isDriverClassnameRowHidden(config), is(false));

        String driverClassnameInputValue = getDriverClassnameInputValue(config);
        assertThat(driverClassnameInputValue, is(driverClassname));

    }

    private HtmlForm clickAdvanceConfig() throws IOException, SAXException {
        HtmlPage htmlPage = webClient.goTo(project.getUrl() + "/configure");
        HtmlForm config = htmlPage.getFormByName("config");
        ((HtmlElement) config.getByXPath("//div[@class='advancedLink']//button").get(0)).click();
        return config;
    }

    private static String getDriverClassnameInputValue(HtmlForm config) {
        List<?> inputs = config.getByXPath("//*[@name='_.driverClassname']");
        HtmlInput driverClassnameInput = (HtmlInput) inputs.get(0);
        return driverClassnameInput.getValueAttribute();
    }

    private static boolean isDriverClassnameRowHidden(HtmlForm config) {
        List<?> tableRows = config.getByXPath("//*[@name='_.driverClassname']/../..");
        assertThat(tableRows, not(empty()));
        HtmlElement row = (HtmlElement) tableRows.get(0);
        LOG.debug("row, maybe? {}" , row);
        return row.getAttribute("style").contains("display: none;");
    }
}
