package org.jenkinsci.plugins.liquibase.builder;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.liquibase.install.LiquibaseInstallation;
import org.kohsuke.stapler.AncestorInPath;

import java.util.Arrays;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;

public abstract class AbstractLiquibaseDescriptor extends BuildStepDescriptor<Builder> {

    public AbstractLiquibaseDescriptor(Class<? extends Builder> clazz) {
        super(clazz);
        load();
    }

    public AbstractLiquibaseDescriptor() {
        super();
        load();
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Project project) {
        return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(anyOf(
                        instanceOf(UsernamePasswordCredentials.class)),
                        CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, project));
    }

    public LiquibaseInstallation[] getInstallations() {
        return Jenkins.get().getDescriptorByType(LiquibaseInstallation.DescriptorImpl.class).getInstallations();
    }

    public void setInstallations(LiquibaseInstallation... installations) {
        Jenkins.get().getDescriptorByType(LiquibaseInstallation.DescriptorImpl.class).setInstallations(installations);

    }

}
