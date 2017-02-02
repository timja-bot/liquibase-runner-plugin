freeStyleJob('@JOB_NAME@') {
  customWorkspace('@WORKSPACE@')
  steps {
    liquibaseRollback {
      changeLogFile('sunny-day-changeset.xml')
      // in practice only one type of rollback would be included.  They appear here for testing pruposes.
      rollbackCount(2)
      rollbackToTag('tag')
      rollbackToDate('13/10/1973 8:00')
      rollbackLastHours(1)

    }
  }
}
