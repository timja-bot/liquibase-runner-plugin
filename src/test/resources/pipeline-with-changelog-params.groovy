node {
  ws('@WORKSPACE@') {

    liquibaseUpdate(changeLogFile: 'with-changelog-property.xml',
            changeLogParameters: ['sample.table.name=@PARAM_VALUE@'])
  }
}

