node {
  ws('@WORKSPACE@') {

    liquibaseUpdate(changeLogFile: 'sunny-day-changeset.xml', testRollbacks: true)
  }
}

