node {
  liquibaseUpdate(
          changeLogFile: 'changeset.yml',
          testRollbacks: true,
          url: 'jdbc:postgresql://localhost:5432/sample-db',
          driverClassname: 'org.postgresql.Driver',
          credentialsId: 'database_password_credentials_id',
          liquibasePropertiesPath: '/etc/liquibase.properties',
          contexts: 'staging',
          changeLogParameters: [          // changelog parameters are a list of strings,
                  'color=blue',           // each in the form of [parameter=value].
                  'shape=circle'
          ]


  )
  liquibaseRollback(changeLogFile: 'changeset.yml',
          rollbackToTag: '',
          url: 'jdbc:postgresql://localhost:5432/sample-db',
          driverClassname: 'org.postgresql.Driver',
          credentialsId: 'database_password_credentials_id',
          liquibasePropertiesPath: '/etc/liquibase.properties'
  )
}
