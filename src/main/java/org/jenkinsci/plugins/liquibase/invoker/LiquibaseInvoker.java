package org.jenkinsci.plugins.liquibase.invoker;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import jenkins.tasks.SimpleBuildStep;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.liquibase.evaluator.AbstractLiquibaseBuilder;
import org.jenkinsci.plugins.liquibase.evaluator.ExecutedChangesetAction;

public class LiquibaseInvoker extends AbstractLiquibaseBuilder implements SimpleBuildStep {

    private String command;

    protected boolean testRollbacks;
    private boolean dropAll;
    protected boolean tagOnSuccessfulBuild;


    @Override
    public void runPerform(Run<?, ?> build,
                           TaskListener listener,
                           Liquibase liquibase,
                           Contexts contexts,
                           LabelExpression labelExpression,
                           ExecutedChangesetAction executedChangesetAction,
                           FilePath workspace) throws InterruptedException, IOException, LiquibaseException {

    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return null;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {

        ArgumentListBuilder cliCommand = new ArgumentListBuilder();

        if (changeLogFile != null) {
            cliCommand.add("--changeLogFile=").addQuoted(changeLogFile);
        }
        if (liquibasePropertiesPath!=null) {
            cliCommand.add("--defaultsFile", liquibasePropertiesPath);
        }

        cliCommand.add(command);




        int exitStatus = launcher.launch().cmds(cliCommand).stdout(listener).join();





        /**
         *  status [--verbose]        Outputs count (list if --verbose) of unrun changesets
         unexpectedChangeSets [--verbose]
         Outputs count (list if --verbose) of changesets run
         --changeLogFile=<path and filename>        Migration file
         --username=<value>                         Database username
         --password=<value>                         Database password. If values
         --url=<value>                              Database URL
         --classpath=<value>                        Classpath containing
         --driver=<jdbc.driver.ClassName>           Database driver class name
         --databaseClass=<database.ClassName>       custom liquibase.database.Database
         --propertyProviderClass=<properties.ClassName>  custom Properties
         --defaultSchemaName=<name>                 Default database schema to use
         --contexts=<value>                         ChangeSet contexts to execute
         --labels=<expression>                      Expression defining labeled
         --defaultsFile=</path/to/file.properties>  File with default option values
         --delimiter=<string>                       Used with executeSql command to set
         --driverPropertiesFile=</path/to/file.properties>  File with custom properties
         --changeExecListenerClass=<ChangeExecListener.ClassName>     Custom Change Exec
         --changeExecListenerPropertiesFile=</path/to/file.properties> Properties for
         --liquibaseCatalogName=<name>              The name of the catalog with the
         --liquibaseSchemaName=<name>               The name of the schema with the
         --databaseChangeLogTableName=<name>        The name of the Liquibase ChangeLog
         --databaseChangeLogLockTableName=<name>    The name of the Liquibase ChangeLog
         --liquibaseSchemaName=<name>               The name of the schema with the
         --includeSystemClasspath=<true|false>      Include the system classpath
         --promptForNonLocalDatabase=<true|false>   Prompt if non-localhost
         --logLevel=<level>                         Execution log level
         --logFile=<file>                           Log file
         --currentDateTimeFunction=<value>          Overrides current date time function
         --outputDefaultSchema=<true|false>         If true, SQL object references
         --outputDefaultCatalog=<true|false>        If true, SQL object references
         --outputFile=<file>                        File to write output to for commands
         --help                                     Prints this message
         --version                                  Prints this version information
         --referenceUsername=<value>                Reference Database username
         --referencePassword=<value>                Reference Database password. If
         --referenceUrl=<value>                     Reference Database URL
         --defaultCatalogName=<name>                Default database catalog to use
         --defaultSchemaName=<name>                 Default database schema to use
         --referenceDefaultCatalogName=<name>       Reference database catalog to use
         --referenceDefaultSchemaName=<name>        Reference database schema to use
         --schemas=<name1,name2>                    Database schemas to include
         --referenceSchemas=<name1,name2>           Reference database schemas to
         --schemas
         --outputSchemaAs=<name1,name2>             On diffChangeLog/generateChangeLog,
         --includeCatalog=<true|false>              If true, the catalog will be
         --includeSchema=<true|false>               If true, the schema will be
         --referenceDriver=<jdbc.driver.ClassName>  Reference database driver class name
         --dataOutputDirectory=DIR                  Output data as CSV in the given
         --diffTypes                                List of diff types to include in

         */


    }
}
