package org.jenkinsci.plugins.liquibase.builder;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;

import org.jenkinsci.plugins.liquibase.installation.LiquibaseInstallation;

@Extension
public final class LiquibaseStepDescriptor extends BuildStepDescriptor<Builder> {
    private volatile LiquibaseInstallation[] installations = new LiquibaseInstallation[0];

    public LiquibaseStepDescriptor() {
        super(LiquibaseBuilder.class);
        load();
    }

    public LiquibaseInstallation[] getInstallations() {
        return installations;
    }

    public void setInstallations(LiquibaseInstallation... installations) {
        this.installations = installations;
        save();
    }
    public LiquibaseInstallation.DescriptorImpl getToolDescriptor() {
        return ToolInstallation.all().get(LiquibaseInstallation.DescriptorImpl.class);
    }

    @Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Invoke Liquibase";
    }
}
