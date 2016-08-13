package org.jenkinsci.plugins.liquibase.evaluator;

import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.util.List;

import com.google.common.collect.Lists;

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

    private void initDriverList() {
        includedDatabaseDrivers = Lists.newArrayList(new IncludedDatabaseDriver("MySQL", "com.mysql.jdbc.Driver"),
                new IncludedDatabaseDriver("PostgreSQL", "org.postgresql.Driver"),
                new IncludedDatabaseDriver("Derby", "org.apache.derby.jdbc.EmbeddedDriver"),
                new IncludedDatabaseDriver("Hypersonic", "org.hsqldb.jdbcDriver"),
                new IncludedDatabaseDriver("H2", "org.h2.Driver"));
    }
}
