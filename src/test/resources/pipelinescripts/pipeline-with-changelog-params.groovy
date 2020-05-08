package pipelinescripts;

node {
  ws('@WORKSPACE@') {

    liquibaseUpdate(changeLogFile: 'example-changesets/with-changelog-property.xml',
            changeLogParameterList: ['sample.table.name=@PARAM_VALUE@'])
  }
}

