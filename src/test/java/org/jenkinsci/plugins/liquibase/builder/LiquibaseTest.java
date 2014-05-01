package org.jenkinsci.plugins.liquibase.builder;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.FileSystemResourceAccessor;

import org.junit.Test;

public class LiquibaseTest {

    @Test
    public void testAccess() throws LiquibaseException {

        Database databaseObject = CommandLineUtils.createDatabaseObject(getClass().getClassLoader(),
                "jdbc:mysql://localhost:3306/sampledb?zeroDateTimeBehavior=convertToNull&characterEncoding=utf8&characterSetResults=utf8",
                "root", "", "com.mysql.jdbc.Driver", null, null, true, true, null, null, null, null);
        Liquibase liquibase = new Liquibase("changeset.xml", new FileSystemResourceAccessor("/home/keith/projects/sample_liquibase"), databaseObject);
        liquibase.listUnrunChangeSets("test");
    }


}