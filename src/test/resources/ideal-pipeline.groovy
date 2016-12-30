node {
  ws('/home/keith/projects/liquibase-runner-plugin/src/test/resources/example-changesets') {

    liquibaseUpdate(changeLogFile: 'sunny-day-changeset.xml', testRollbacks: true)
  }
}

