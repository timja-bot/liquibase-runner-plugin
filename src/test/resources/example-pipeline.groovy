node {
  liquibaseUpdate(
          changeLogFile: 'changeset.yml',
          testRollbacks: true,
          url: 'jdbc:postgresql://localhost:5432/sample-db',
          driverClassname: 'org.postgresql.Driver',
          // instead of driverClassname, you can set databaseEngine to MySQL, Derby, Postgres, Derby, or Hypersonic
          databaseEngine: 'MySQL',
          credentialsId: 'database_password_credentials_id',
          liquibasePropertiesPath: '/etc/liquibase.properties',
          contexts: 'staging',
          changeLogParameterList: [          // changelog parameters are a list of strings,
                  'color=blue',              // each in the form of [parameter=value].
                  'shape=circle'
          ],
          changeLogParameters:  'subject=english'   // alternatively, changelog parameters can be
                                                    // supplied as a string, each key/value pair on a new line

  )
  liquibaseRollback(changeLogFile: 'changeset.yml',
          rollbackToTag: 'deploy-2.5',
          url: 'jdbc:postgresql://localhost:5432/sample-db',
          driverClassname: 'org.postgresql.Driver',
          credentialsId: 'database_password_credentials_id',
          liquibasePropertiesPath: '/etc/liquibase.properties'
  )

  liquibaseUpdate('changeset.xml')  // using minimum configuration will cause the plugin to use an H2 inmemory database.
}
