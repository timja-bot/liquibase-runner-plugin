package org.jenkinsci.plugins.liquibase.builder;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.model.Item;
import hudson.model.Project;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.liquibase.install.LiquibaseInstallation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

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

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item,
                                                 @QueryParameter String credentialsId,
                                                 @AncestorInPath Project project) {
        StandardListBoxModel result = new StandardListBoxModel();

        if (item == null) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return result.includeCurrentValue(credentialsId);
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ)
                    && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId);
            }
        }
        return result
                .includeEmptyValue()
                .withMatching(anyOf(
                        instanceOf(UsernamePasswordCredentials.class)),
                        CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, project))
                .includeCurrentValue(credentialsId);
    }

    public LiquibaseInstallation[] getInstallations() {
        return Jenkins.get().getDescriptorByType(LiquibaseInstallation.DescriptorImpl.class).getInstallations();
    }

    public void setInstallations(LiquibaseInstallation... installations) {
        Jenkins.get().getDescriptorByType(LiquibaseInstallation.DescriptorImpl.class).setInstallations(installations);

    }

}
