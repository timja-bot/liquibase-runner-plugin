

node {
    stage('build') {
         liquibaseUpdate(changeLogFile: "changeset.xml")
    }
    
}

