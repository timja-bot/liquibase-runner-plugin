node {
  ws('@WORKSPACE@') {

    liquibaseUpdate(changeLogFile: 'with-changelog-property.xml',
            changeLogParameterList: ['sample.table.name=@PARAM_VALUE@'])
  }
}

