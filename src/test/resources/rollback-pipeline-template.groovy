node {
  ws('@WORKSPACE@') {

    liquibaseRollback(changeLogFile: 'sunny-day-changeset.xml', rollbackCount: 2)
  }
}

