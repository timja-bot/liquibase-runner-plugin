package org.jenkinsci.plugins.liquibase.common;

import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

import java.io.IOException;

import org.jenkinsci.plugins.liquibase.evaluator.AbstractLiquibaseBuilder;
import org.jenkinsci.plugins.liquibase.evaluator.ExecutedChangesetAction;

public class BuilderStub extends AbstractLiquibaseBuilder {

    @Override
    public void runPerform(Run<?, ?> build,
                           TaskListener listener,
                           Liquibase liquibase,
                           Contexts contexts,
                           LabelExpression labelExpression,
                           ExecutedChangesetAction executedChangesetAction,
                           FilePath workspace)
            throws InterruptedException, IOException, LiquibaseException {

    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return null;
    }

    @Override
    public String getChangeLogFile() {
        return "${token}";
    }
}
