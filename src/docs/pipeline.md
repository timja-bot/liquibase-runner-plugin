Pipeline Support
================

As of version 1.2.0, the liquibase runner avails itself to pipeline scripts.  Configuration is nearly identical to traditional use of the plugin.
 
Here are a couple of example pipeline scripts that utilize almost all the available options:

Minimal Update
-------
```
node {
  // using minimum configuration will cause the plugin to use an H2 inmemory database.
  liquibaseUpdate('changeset.xml')
}
```

Update with many parameters
-----------
```
node {
  liquibaseUpdate(changeLogFile: 'changeset.yml',
          testRollbacks: true,
          url: 'jdbc:postgresql://localhost:5432/sample-db',
          driverClassname: 'org.postgresql.Driver',
          classpath: 'path/to/additional/classes', // may be relative or absolute
          // instead of driverClassname, you can set databaseEngine to MySQL, Derby, Postgres, Derby, or Hypersonic
          databaseEngine: 'MySQL',
          credentialsId: 'database_password_credentials_id',
          liquibasePropertiesPath: '/etc/liquibase.properties',
          contexts: 'staging',
          changeLogParameterList: ['color=blue',     // changelog parameters are a list of strings,
                                   'shape=circle'],  // each in the format of 'parameter=value'.
          // alternatively, changelog parameters can be supplied as a single string,
          // each key / value pair on a new line
          changeLogParameters: ''''subject=english
                                   planet=earth'''
  )
}
```

Rollback
---------
```
node {
  // rollback has many of the same parameters as update, with additional ones that control rollback behavior.
  // Note that the below example has mutually exclusive rollback options.  In practice, you'd either specify
  // rollbackCount, rollbackToTag, rollbackToDate, or rollbackLastHours.
  liquibaseRollback(changeLogFile: 'changeset.yml',
          url: 'jdbc:postgresql://localhost:5432/sample-db',
          driverClassname: 'org.postgresql.Driver',
          credentialsId: 'database_password_credentials_id',
          liquibasePropertiesPath: '/etc/liquibase.properties',
          rollbackToTag: 'deploy-2.5',
          rollbackCount: 2,
          rollbackToDate: "2016-10-13'T'12:00:00",
          rollbackLastHours: 3
  )
}
```

DbDoc Generation
-----------------

```
node {
   liquibaseDbDoc( changeLogFile: 'changeset.yml',   // same basic configuration parameters
                   url: 'jdbc:postgresql://localhost:5432/sample-db',
                   driverClassname: 'org.postgresql.Driver',
                   outputDirectory: 'dbdoc')
                   
   // works great with the HTML publisher plugin (sold separately)                   
   publishHTML(target: [reportDir  : 'dbdoc',
                        reportFiles: 'index.html',
                        reportName : 'DbDoc',
                        keepAll    : true])               
                   

}
```