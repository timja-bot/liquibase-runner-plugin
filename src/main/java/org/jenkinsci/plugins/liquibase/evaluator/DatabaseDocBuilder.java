package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

import java.io.File;
import java.io.IOException;

import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class DatabaseDocBuilder extends AbstractLiquibaseBuilder {

    private String outputDirectory;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public DatabaseDocBuilder(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void runPerform(Run<?, ?> build,
                           TaskListener listener,
                           Liquibase liquibase,
                           Contexts contexts,
                           LabelExpression labelExpression,
                           ExecutedChangesetAction executedChangesetAction, FilePath workspace)
            throws InterruptedException, IOException, LiquibaseException {


        executedChangesetAction.setNoExecutionsExpected(true);
        FilePath filePath = workspace.child(outputDirectory);

        listener.getLogger().println("Generating Liquibase dbDoc in directory '" + outputDirectory + "'");

        FilePath.FileCallable callable = new DatabaseDocGenerationCallback(liquibase, contexts, labelExpression);

        filePath.act(callable);

    }
    private static class DatabaseDocGenerationCallback implements FilePath.FileCallable<Void> {

        Liquibase liquibase;
        Contexts contexts;
        LabelExpression labelExpression;

        public DatabaseDocGenerationCallback(Liquibase liquibase, Contexts contexts, LabelExpression labelExpression) {
            this.liquibase = liquibase;
            this.contexts = contexts;
            this.labelExpression = labelExpression;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            f.mkdirs();
            try {
                liquibase.generateDocumentation(f.getAbsolutePath(), contexts, labelExpression);
            } catch (LiquibaseException e) {
                throw new IOException("Error generating documentation", e);
            }
            return null;
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {

        }
    }

    @DataBoundSetter
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    public static class DescriptorImpl extends AbstractLiquibaseDescriptor {

        public DescriptorImpl() {
            load();
        }

        public DescriptorImpl(Class<? extends DatabaseDocBuilder> clazz) {
            super(clazz);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Generate Liquibase dbDoc";
        }
    }
}
