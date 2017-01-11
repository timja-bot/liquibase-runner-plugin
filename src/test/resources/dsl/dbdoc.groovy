freeStyleJob('@JOB_NAME@') {
  customWorkspace('@WORKSPACE@')
  steps {
    liquibaseDbDoc {
      changeLogFile('sunny-day-changeset.xml')
      outputDirectory('dbdoc')

    }
  }
}
