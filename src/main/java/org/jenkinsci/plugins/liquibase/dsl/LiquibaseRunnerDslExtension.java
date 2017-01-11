package org.jenkinsci.plugins.liquibase.dsl;

import hudson.Extension;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

import org.jenkinsci.plugins.liquibase.evaluator.AbstractLiquibaseBuilder;
import org.jenkinsci.plugins.liquibase.evaluator.ChangesetEvaluator;
import org.jenkinsci.plugins.liquibase.evaluator.DatabaseDocBuilder;
import org.jenkinsci.plugins.liquibase.evaluator.RollbackBuilder;

@SuppressWarnings("MethodMayBeStatic")
@Extension(optional = true)
public class LiquibaseRunnerDslExtension extends ContextExtensionPoint {

    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "liquibase-runner", minimumVersion = "1.3.0")
    public Object liquibaseUpdate(Runnable closure) {
        ChangesetEvaluator builder = new ChangesetEvaluator();
        LiquibaseContext context = composeContext(closure);

        setCommonBuilderProperties(builder, context);
        builder.setTagOnSuccessfulBuild(context.isTagOnSuccessfulBuild());
        builder.setTestRollbacks(context.isTestRollbacks());
        builder.setDropAll(context.isDropAll());

        return builder;
    }

    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "liquibase-runner", minimumVersion = "1.3.0")
    public Object liquibaseRollback(Runnable closure) {
        LiquibaseContext context = composeContext(closure);
        RollbackBuilder rollbackBuilder = new RollbackBuilder();
        setCommonBuilderProperties(rollbackBuilder, context);
        if (context.getRollbackCount()!=null) {
            rollbackBuilder.setNumberOfChangesetsToRollback(String.valueOf(context.getRollbackCount()));
            rollbackBuilder.setRollbackType(RollbackBuilder.RollbackStrategy.COUNT.name());
        }
        if (context.getRollbackToTag() != null) {
            rollbackBuilder.setRollbackToTag(context.getRollbackToTag());
            rollbackBuilder.setRollbackType(RollbackBuilder.RollbackStrategy.TAG.name());
        }
        if (context.getRollbackToDate() != null) {
            rollbackBuilder.setRollbackToDate(context.getRollbackToDate());
            rollbackBuilder.setRollbackType(RollbackBuilder.RollbackStrategy.DATE.name());
        }

        if (context.getRollbackLastHours()!=null) {
            rollbackBuilder.setRollbackLastHours(String.valueOf(context.getRollbackLastHours()));
            rollbackBuilder.setRollbackType(RollbackBuilder.RollbackStrategy.RELATIVE.name());
        }
        return rollbackBuilder;
    }

    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "liquibase-runner", minimumVersion = "1.3.0")
    public Object liquibaseDbDoc(Runnable closure) {
        LiquibaseContext context = composeContext(closure);
        DatabaseDocBuilder builder = new DatabaseDocBuilder(context.getOutputDirectory());
        setCommonBuilderProperties(builder, context);

        return builder;
    }

    private static LiquibaseContext composeContext(Runnable closure) {
        LiquibaseContext context = new LiquibaseContext();
        executeInContext(closure, context);
        return context;
    }

    private static void setCommonBuilderProperties(AbstractLiquibaseBuilder builder, LiquibaseContext context) {
        builder.setChangeLogParameters(context.composeChangeLogString());
        builder.setChangeLogFile(context.getChangeLogFile());
        builder.setUrl(context.getUrl());
        builder.setDefaultSchemaName(context.getDefaultSchemaName());
        builder.setContexts(context.getContexts());
        builder.setLiquibasePropertiesPath(context.getLiquibasePropertiesPath());
        builder.setClasspath(context.getClasspath());
        builder.setDriverClassname(context.getDriverClassname());
        builder.setLabels(context.getLabels());
        builder.setCredentialsId(context.getCredentialsId());
        builder.setBasePath(context.getBasePath());
        if (context.getDatabaseEngine() != null) {
            builder.setUseIncludedDriver(true);
        }

    }
}
