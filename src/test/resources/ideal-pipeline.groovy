node {
  writeFile file: 'changeset.yml', text: changelog()

  liquibaseUpdate(changeLogFile: 'changeset.yml', testRollbacks: true,
          driverClassname: 'org.h2.Driver',
          url: 'jdbc:h2:mem:builder-db')
}

def changelog() {
  """
databaseChangeLog:
  - changeSet:
      id: shape-table-create
      author: keith
      changes:
        - createTable:
            tableName: shape
            columns:
              - column:
                  name: id
                  type: bigint
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: varchar(50)
                  constraints:
                    nullable: false
              - column:
                  name: description
                  type: varchar(50)

"""

}