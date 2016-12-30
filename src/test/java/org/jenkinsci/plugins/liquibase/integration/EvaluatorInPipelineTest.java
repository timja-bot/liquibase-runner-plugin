package org.jenkinsci.plugins.liquibase.integration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.javascript.host.html.HTMLElement;

public class EvaluatorInPipelineTest {
    private static final Logger LOG = LoggerFactory.getLogger(EvaluatorInPipelineTest.class);

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    protected JenkinsRule.WebClient webClient;
    protected String pipelineScript;

    @Before
    public void setup() throws IOException {
        webClient = jenkinsRule.createWebClient();

        InputStream resourceAsStream = getClass().getResourceAsStream("/sample-pipline.groovy");
        pipelineScript = IOUtils.toString(resourceAsStream);
    }

    @Ignore("can't get the damn webclient to submit form.  Think i'll use selenium instead")
    @Test
    public void should_allow_pipeline() throws IOException, SAXException {

        URL url = jenkinsRule.getURL();

        LOG.debug("jenkins url: {} ", url.toString());

        HtmlPage newJobPage = webClient.goTo("newJob");
        HtmlForm createItem = newJobPage.getFormByName("createItem");
        HtmlInput nameInput = createItem.getInputsByName("name").get(0);
        String jobName = RandomStringUtils.randomAlphabetic(5);
        nameInput.type(jobName);

        List<HtmlRadioButtonInput> modes = createItem.getRadioButtonsByName("mode");

        LOG.debug("mode buttons size:{}", modes.size());

        modes.get(1).setChecked(true);

        newJobPage.getElementById("ok-button").click();

        HtmlPage configurationPage = webClient.goTo("job/" + jobName + "/configure");


        LOG.debug("configuration URL:{}", configurationPage.getUrl().toString());

        HtmlElement editor = configurationPage.getFirstByXPath("//div[@id='workflow-editor-1']");

        LOG.debug("editor:{}", editor);


        List textareas = configurationPage.getByXPath("//textarea");
        HtmlTextArea scriptInput = (HtmlTextArea) textareas.get(textareas.size() - 1);
        scriptInput.type(pipelineScript);

        ((HTMLElement) configurationPage.getFirstByXPath("//span[@name='Submit']")).click();






    }

}
