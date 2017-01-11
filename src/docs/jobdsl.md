Job DSL Integration
===================


As of version 1.3.0 of Liquibase Runner, you can now use the Job DSL plugin to script liquibase job creation.  Here are some examples:

Minimal Update
--------------
Just as with manual configuration, providing merely the changelog file will make the plugin use an in-memory H2 database.
```

job() {
  steps {
    liquibaseUpdate {
      changeLogFile('changeset.xml') 
    }
  }
}

```

Update with many parameters
---------------------------
```
job() {
    steps {
        liquibaseUpdate {
          changeLogFile('changeset.yml')
          testRollbacks(true)
          url('jdbc:postgresql://localhost:5432/sample-db')
          driverClassname('org.postgresql.Driver')
          // instead of driverClassname, you can set databaseEngine to MySQL, Derby, Postgres, Derby, or Hypersonic
          databaseEngine('MySQL')
          credentialsId('database_password_credentials_id')
          liquibasePropertiesPath('/etc/liquibase.properties')
          contexts('staging')  // can be comma delimited list
          // changelog parameters are supplied as a map of key/value pairs
          changeLogParameters( [
                "sample.table.name":"blue",
                "favorite.food":"spaghetti"
               ]
          )
      }
    } 
}
```

Rollback
--------
The same base configuration fields are available for rollbacks, plus additional ones which control rollback behavior.  

Note that the below example has mutually exclusive rollback options.  In practice, you'd either specify rollbackCount, rollbackToTag, rollbackToDate, or rollbackLastHours.
```
job() {
    steps {
        liquibaseUpdate {
            changeLogFile('changeset.yml')
            testRollbacks(true)
            url('jdbc:postgresql://localhost:5432/sample-db')
            driverClassname('org.postgresql.Driver')
            credentialsId('database_password_credentials_id')
            rollbackToTag('deploy-2.5')
            rollbackCount(2)
            rollbackToDate("2016-10-13'T'12:00:00")
            rollbackLastHours(3)
      }
    } 
}
```