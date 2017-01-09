package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ModelObject;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.google.common.base.Strings;

/**
 * Migrates projects that had been using old username/password fields to use credentials.
 */
public class CredentialsMigrator {

    private static final Logger LOG = Logger.getLogger(CredentialsMigrator.class.getName());

    @Extension
    static final public class ItemListenerImpl extends ItemListener {
        @Override
        public void onLoaded() {
            CredentialsMigrator.migrateLegacyCredentials();
        }
    }
    static void migrateLegacyCredentials() {
        LOG.finest("credentials migrator invoked");

        Jenkins instance = Jenkins.getInstance();
        if (instance != null) {
            List<Project> projects = instance.getItems(Project.class);
            for (Project project : projects) {
                DescribableList buildersList = project.getBuildersList();
                List builders = buildersList.getAll(AbstractLiquibaseBuilder.class);
                for (Object builder : builders) {
                    AbstractLiquibaseBuilder liquibaseBuilder = (AbstractLiquibaseBuilder) builder;
                    migrateCredentials(project, liquibaseBuilder);
                }
            }
        }
    }

    protected static void migrateCredentials(Item project, AbstractLiquibaseBuilder liquibaseBuilder) {
        if (liquibaseBuilder.hasLegacyCredentials()) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("found builder needing credentials migration.  project:" +
                        project.getDisplayName());
            }
            try {
                String plainTextPassword = Strings.nullToEmpty(liquibaseBuilder.getPassword());
                String credentialsId = createCredentials(liquibaseBuilder.getUsername(),
                                plainTextPassword,
                                project);
                liquibaseBuilder.setCredentialsId(credentialsId);
                liquibaseBuilder.clearLegacyCredentials();
                project.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String createCredentials(String username, String password, ModelObject project) throws IOException {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("creating new credential for project:" + project.getDisplayName());
        }

        String credentialsId = UUID.randomUUID().toString();
        String description = "migrated from " + project.getDisplayName();
        Credentials credentialsToCreate = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL,
                credentialsId,
                description,
                username,
                password
        );

        SystemCredentialsProvider credentialsProvider = SystemCredentialsProvider.getInstance();
        Map<Domain, List<Credentials>> credentialsMap = credentialsProvider.getDomainCredentialsMap();

        Domain global = Domain.global();
        if (credentialsMap.get(global) == null) {
            credentialsMap.put(global, Collections.<Credentials>emptyList());
        }
        credentialsMap.get(global).add(credentialsToCreate);
        credentialsProvider.setDomainCredentialsMap(credentialsMap);
        credentialsProvider.save();

        return credentialsId;
    }
}
