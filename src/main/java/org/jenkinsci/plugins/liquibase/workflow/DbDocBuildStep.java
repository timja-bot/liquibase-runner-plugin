package org.jenkinsci.plugins.liquibase.workflow;

import hudson.Extension;
import hudson.model.Project;
import hudson.util.ListBoxModel;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.anyOf;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.instanceOf;

public class DbDocBuildStep extends AbstractLiquibaseStep {
    private String outputDirectory = "";


    @DataBoundConstructor
    public DbDocBuildStep(String changeLogFile) {
        super(changeLogFile);
    }

    @DataBoundSetter
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }


    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(DbDocExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "liquibaseDbDoc";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Generate Liquibase DbDoc";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Project project) {
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(anyOf(
                            instanceOf(UsernamePasswordCredentials.class)),
                            CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, project));
        }
    }
}
