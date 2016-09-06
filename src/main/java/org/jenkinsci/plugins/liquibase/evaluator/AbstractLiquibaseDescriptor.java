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
    private List<IncludedDatabaseDriver> includedDatabaseDrivers;

    public AbstractLiquibaseDescriptor(Class<? extends Builder> clazz) {
        super(clazz);
    }

    public AbstractLiquibaseDescriptor() {
        super();
    }

    public List<IncludedDatabaseDriver> getIncludedDatabaseDrivers() {
        if (includedDatabaseDrivers == null) {
            initDriverList();
        }
        return includedDatabaseDrivers;
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Project project) {
        return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(anyOf(
                        instanceOf(UsernamePasswordCredentials.class)),
                        CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, project));
    }

    private void initDriverList() {
        includedDatabaseDrivers = Lists.newArrayList(new IncludedDatabaseDriver("MySQL", "com.mysql.jdbc.Driver"),
                new IncludedDatabaseDriver("PostgreSQL", "org.postgresql.Driver"),
                new IncludedDatabaseDriver("Derby", "org.apache.derby.jdbc.EmbeddedDriver"),
                new IncludedDatabaseDriver("Hypersonic", "org.hsqldb.jdbcDriver"),
                new IncludedDatabaseDriver("H2", "org.h2.Driver"));
    }
}
