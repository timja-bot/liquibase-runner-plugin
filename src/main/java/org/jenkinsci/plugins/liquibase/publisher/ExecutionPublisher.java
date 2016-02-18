package org.jenkinsci.plugins.liquibase.publisher;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;

import org.jenkinsci.plugins.liquibase.builder.ChangeSetAction;
import org.jenkinsci.plugins.liquibase.builder.ExecutedChangesetAction;
import org.jenkinsci.plugins.liquibase.builder.LiquibaseBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Splitter;

public class ExecutionPublisher extends Recorder {

    @DataBoundConstructor
    public ExecutionPublisher() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        LiquibaseBuilder.createLiquibase(build, listener, new ExecutedChangesetAction(), null);
        FilePath child = build.getWorkspace().child("liqibase.execution.json");
        String executionLogEntries = child.readToString();
        Iterable<String> entries = Splitter.on("\n").omitEmptyStrings().split(executionLogEntries);
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(ChangeSetAction.class, new ChangeSetActionDeserializer());

        mapper.registerModule(simpleModule);
        ChangesetListAction changesetListAction = new ChangesetListAction();
        ExecutedChangesetAction executedChangesetAction = new ExecutedChangesetAction();
        for (String entry : entries) {
            ChangeSetAction changeSetAction = mapper.readValue(entry, ChangeSetAction.class);
            executedChangesetAction.addChangeSetAction(changeSetAction);
        }
        build.addAction(executedChangesetAction);

        return true;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish liquibase execution results.";
        }
    }

    private static class ChangeSetActionDeserializer extends JsonDeserializer<ChangeSetAction> {
        @Override
        public ChangeSetAction deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException, JsonProcessingException {
            ObjectCodec oc = jsonParser.getCodec();
            JsonNode node = oc.readTree(jsonParser);
            ChangeSetAction changeSetAction = new ChangeSetAction();

            for (JsonNode next : node.get("sqls")) {
                changeSetAction.addSql(next.asText());
            }
            changeSetAction.setId(node.get("id").asText());
            if (node.get("author") != null) {
                changeSetAction.setAuthor(node.get("author").asText());
            }
            changeSetAction.setComment(node.get("comment").asText());
            changeSetAction.setExecutionTime(node.get("executionTime").asText());
            changeSetAction.setResult(node.get("result").asText());
            return changeSetAction;

        }
    }
}
