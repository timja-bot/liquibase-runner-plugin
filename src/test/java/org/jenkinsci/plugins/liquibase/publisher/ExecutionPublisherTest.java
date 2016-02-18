package org.jenkinsci.plugins.liquibase.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.liquibase.builder.ChangeSetAction;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ExecutionPublisherTest {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionPublisherTest.class);

    @Test
    public void test_parser() throws IOException {

        InputStream resourceAsStream = getClass().getResourceAsStream("/execution.json");
        List<String> lines = IOUtils.readLines(resourceAsStream);

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();

        simpleModule.addDeserializer(ChangeSetAction.class, new JsonDeserializer<ChangeSetAction>() {
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
                if (node.get("author")!=null) {
                    changeSetAction.setAuthor(node.get("author").asText());
                }
                changeSetAction.setComment(node.get("comment").asText());
                changeSetAction.setExecutionTime(node.get("executionTime").asText());
                changeSetAction.setResult(node.get("result").asText());
                return changeSetAction;
            }
        });
        mapper.registerModule(simpleModule);
        for (String line : lines) {
            ChangeSetAction changeSetAction = mapper.readValue(line, ChangeSetAction.class);
            if(LOG.isDebugEnabled()) {
            	LOG.debug("changeSetAction:[" + changeSetAction + "]");
            }
        }



    }

}