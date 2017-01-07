node {
  // using minimum configuration will cause the plugin to use an H2 inmemory database.
  liquibaseUpdate('changeset.xml')
}

node {
  liquibaseUpdate(changeLogFile: 'changeset.yml',
          testRollbacks: true,
          url: 'jdbc:postgresql://localhost:5432/sample-db',
          driverClassname: 'org.postgresql.Driver',
          // instead of driverClassname, you can set databaseEngine to MySQL, Derby, Postgres, Derby, or Hypersonic
          databaseEngine: 'MySQL',
          credentialsId: 'database_password_credentials_id',
          liquibasePropertiesPath: '/etc/liquibase.properties',
          contexts: 'staging',
          changeLogParameterList: [                  // changelog parameters are a list of strings,
                                   'color=blue',     // each in the format of [parameter=value].
                                   'shape=circle'],
          // alternatively, changelog parameters can be supplied as a single string,
          // each key / value pair on a new line
          changeLogParameters: ''''subject=english
                                   planet=earth'''
  )
}

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

node {
  // generate db doc
  liquibaseDbDoc(changeLogFile: 'changeset.yml', outputDirectory: 'dbdoc')
  // publishing requires installation of the PublishHTML plugin:
  publishHTML(target: [reportDir  : 'dbdoc',
                       reportFiles: 'index.html',
                       reportName : 'DbDoc',
                       keepAll    : true])

}
