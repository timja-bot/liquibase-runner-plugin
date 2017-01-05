node {
  ws('@WORKSPACE@') {

    liquibaseDbDoc(changeLogFile: 'sunny-day-changeset.xml', outputDirectory: "doc")

  }
}

