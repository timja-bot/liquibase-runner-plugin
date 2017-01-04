node {
  ws('/home/keith/projects/liquibase-runner-plugin/src/test/resources/example-changesets') {
    // liquibaseUpdate()
    step([$class: 'ChangesetEvaluator', changeLogFile: 'sunny-day-changeset.xml', testRollbacks: true])
  }
}

