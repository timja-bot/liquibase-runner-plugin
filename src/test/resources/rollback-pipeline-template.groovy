node {
  ws('@WORKSPACE@') {
    liquibaseUpdate(url: '@DB_URL@',
            changeLogFile: 'sunny-day-changeset.xml')

    liquibaseRollback(
            url: '@DB_URL@',
            changeLogFile: 'sunny-day-changeset.xml', rollbackCount: 2)
  }
}

