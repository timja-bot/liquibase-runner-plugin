# Jenkins Liquibase Runner Plugin
---

# About

Adds Liquibase as an available build step.  See Liquibase documentation at http://www.liquibase.org/.

# Installation

*  Install the liquibase-runner plugin using Jenkins' plugin manager.  If you intend to use the "Execute" build step,
you'll also need to install liquibase (the two types of build steps are explained below).

# Configuration

## Build steps: "Execute Liquibase" versus "Evaluate changesets"

This plugin has two modes for running liquibase.  The first invokes an liquibase installation just as you would via command-line.
The second evaluates liquibase changesets internally, and requires no existing liquibase installation.    

In each mode, basic liquibase options, such as contexts and jdbc URL, are provided as configuration options.

Further details are provided below.

**Execute Liquibase**

Use this build step if you have an existing liquibase installation and wish to have it run just as if you were doing so 
from command line.  This mode provides the most flexibility for your liquibase configuration and execution, but provides 
fewer reporting features.

Use "Manage Jenkins" to add your liquibase installation to Jenkins.  It will then be available for selection in the
build step.  Your liquibase installation should include the driver for your database (for example, by having the
driver jar in LIQUIBASE_HOME/lib directory).

Each build's console log will contain the stdout/stderr output of liquibase execution.  Furthermore, each type
of build step will indicate build instability when liquibase is unable to apply a changeset.

**Evaluate changesets**

The "Evaluate liquibase changesets" build step runs liquibase internally and doesn't require liquibase to be installed.  
In addition, build summaries include a list of changesets executed.
 
However, using this mode contains a few restrictions:
  * You cannot choose what liquibase command to run; only "update" and "updateTestingRollback" are used.
  * You are restricted to one of the database engines available in the build step's dropdown.
 
# Usage Tips

* If you're using the H2 database engine, you can use JDBC url like "jdbc:h2:mem:test" to create a in-memory database
with each execution.  If you'd like to have the schema persist between builds, use a jdbc url like 
"jdbc:h2:file:./data/sample", so that the schema is persisted on disk.  No username/password needed. 

* If you'd like to use a database engine like MySQL or postgres, you'll have to ensure the schema exists and is 
accessible to Jenkins.  Consider having your Jenkins job execute an initialize script to do so.

# The Future
  
I'd like to minimize the difference between "Execute" and "Evaluate" and have changesets be reported in both types of
build steps.
 





