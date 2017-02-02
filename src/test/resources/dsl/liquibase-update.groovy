freeStyleJob('@JOB_NAME@') {
  customWorkspace('@WORKSPACE@')
  steps {
    liquibaseUpdate {
      changeLogFile('sunny-day-changeset.xml')

    }
  }
}
