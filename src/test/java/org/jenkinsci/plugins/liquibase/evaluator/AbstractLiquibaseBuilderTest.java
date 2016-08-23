package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.LiquibaseException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.RandomStringUtils;
import org.jenkinsci.plugins.liquibase.integration.LiquibaseTestUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AbstractLiquibaseBuilderTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLiquibaseBuilderTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    Build build;

    @Mock
    BuildListener buildListener;

    @Mock
    Launcher launcher;

    protected String dbUrl;

    @Before
    public void setup() throws IOException {
        temporaryFolder.create();
        File dbfiles = temporaryFolder.newFolder("dbfiles");
        File dbFile = new File(dbfiles, "h2");
        dbUrl = "jdbc:h2:file:" + dbFile.getAbsolutePath();
        when(buildListener.getLogger()).thenReturn(System.out);
    }

    @Test
    public void should_load_changeset_from_system_classpath() throws IOException, LiquibaseException {
        AbstractLiquibaseBuilder liquibaseBuilder = new LiquibaseBuilderStub();

        Properties configProperties = createLiquibaseProperties();
        // this changelogFile is available only through this test class's classloader
        configProperties.setProperty("changeLogFile", "example-changesets/single-changeset.xml");

        Liquibase liquibase = liquibaseBuilder
                .createLiquibase(build, buildListener, new ExecutedChangesetAction(build), configProperties, launcher);
        liquibase.update(new Contexts());

        assertThatOneChangesetExecuted(liquibase);
    }

    @Test
    public void should_load_changeset_from_dynamic_classpath() throws IOException, LiquibaseException {
        Properties liquibaseProperties = createLiquibaseProperties();
        LiquibaseBuilderStub stub = new LiquibaseBuilderStub();

        File changesets = temporaryFolder.newFolder("changesets");

        String subDirectoryName = RandomStringUtils.randomAlphabetic(5);
        File randomSubDir = new File(changesets, subDirectoryName);

        LiquibaseTestUtil.createFileFromResource(randomSubDir, "/example-changesets/single-changeset.xml");

        String changeLogResourcePath = subDirectoryName + "/single-changeset.xml";
        liquibaseProperties.setProperty("changeLogFile", changeLogResourcePath);
        stub.setClasspath(changesets.getAbsolutePath());

        Liquibase liquibase =
                stub.createLiquibase(build, buildListener, new ExecutedChangesetAction(build), liquibaseProperties,
                        launcher);
        liquibase.update(new Contexts(""));

        assertThatOneChangesetExecuted(liquibase);

    }

    private static void assertThatOneChangesetExecuted(Liquibase liquibase) throws LiquibaseException {
        DatabaseChangeLog databaseChangeLog = liquibase.getDatabaseChangeLog();
        List<ChangeSet> changeSets = databaseChangeLog.getChangeSets();
        assertThat(changeSets, hasSize(1));
    }

    private Properties createLiquibaseProperties() throws IOException {
        Properties configProperties = new Properties();
        configProperties.load(getClass().getResourceAsStream("/example-changesets/unit-test.h2.liquibase.properties"));
        configProperties.setProperty("url", dbUrl);
        return configProperties;
    }

    private static class LiquibaseBuilderStub extends AbstractLiquibaseBuilder {
        @Override
        public void doPerform(AbstractBuild<?, ?> build,
                              BuildListener listener,
                              Liquibase liquibase,
                              Contexts contexts,
                              ExecutedChangesetAction executedChangesetAction)
                throws InterruptedException, IOException, LiquibaseException {

        }

        @Override
        public Descriptor<Builder> getDescriptor() {
            return null;
        }
    }
}