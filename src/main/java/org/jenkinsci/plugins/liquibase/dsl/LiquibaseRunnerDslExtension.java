package org.jenkinsci.plugins.liquibase.dsl;

import hudson.Extension;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

import org.jenkinsci.plugins.liquibase.builder.*;

@SuppressWarnings("MethodMayBeStatic")
@Extension(optional = true)
public class LiquibaseRunnerDslExtension extends ContextExtensionPoint {

    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "liquibase-runner", minimumVersion = "1.3.0")
    public Object liquibaseUpdate(Runnable closure) {
        UpdateBuilder builder = new UpdateBuilder();
        LiquibaseContext context = composeContext(closure);
        setCommonBuilderProperties(builder, context);

        return builder;
    }

    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "liquibase-runner", minimumVersion = "1.3.0")
    public Object liquibaseRollback(Runnable closure) {
        RollbackBuilder rollbackBuilder = new RollbackBuilder();
        LiquibaseContext context = composeContext(closure);
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
    public Object liquibaseTag(Runnable closure) {
        TagBuilder tagBuilder = new TagBuilder();
        LiquibaseContext context = composeContext(closure);
        setCommonBuilderProperties(tagBuilder, context);

        tagBuilder.setTag(context.getTag());

        return tagBuilder;
    }

    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "liquibase-runner", minimumVersion = "1.3.0")
    public Object liquibaseDropAll(Runnable closure) {
        DropAllBuilder builder = new DropAllBuilder();
        LiquibaseContext context = composeContext(closure);
        setCommonBuilderProperties(builder, context);

        return builder;
    }

    @DslExtensionMethod(context = StepContext.class)
    @RequiresPlugin(id = "liquibase-runner", minimumVersion = "1.3.0")
    public Object liquibaseCli(Runnable closure) {
        RawCliBuilder tagBuilder = new RawCliBuilder();
        LiquibaseContext context = composeContext(closure);
        setCommonBuilderProperties(tagBuilder, context);

        tagBuilder.setCommandArguments(context.getCommandArguments());

        return tagBuilder;
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
        builder.setContexts(context.getContexts());
        builder.setLiquibasePropertiesPath(context.getLiquibasePropertiesPath());
        builder.setLabels(context.getLabels());
        builder.setCredentialsId(context.getCredentialsId());
        builder.setResourceDirectories(context.getResourceDirectories());
    }
}
