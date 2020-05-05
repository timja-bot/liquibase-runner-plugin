package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import java.util.List;

import org.kohsuke.stapler.AncestorInPath;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.google.common.collect.Lists;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;

public abstract class AbstractLiquibaseDescriptor extends BuildStepDescriptor<Builder> {

    public AbstractLiquibaseDescriptor(Class<? extends Builder> clazz) {
        super(clazz);
    }

    public AbstractLiquibaseDescriptor() {
        super();
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Project project) {
        return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(anyOf(
                        instanceOf(UsernamePasswordCredentials.class)),
                        CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, project));
    }
}
