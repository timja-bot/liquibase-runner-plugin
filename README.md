# Jenkins Liquibase Plugin
---

# About

Adds Liquibase as an available build step.  See Liquibase documentation at http://www.liquibase.org/.

# Installation

*  Install the liquibase-runner plugin.

# Configuration

## Build steps: "Execute Liquibase" versus "Evaluate changesets"

This plugin has two modes for running liquibase.  The first invokes an liquibase installation just as you would via command-line.
The second evaluates liquibase changesets internally, and requires no existing liquibase installation.    

In each mode, basic liquibase options, such as contexts and jdbc URL, are provided as configuration options.

Further details are provided below.

**Execute Liquibase**

Use this build step if you have an existing liquibase installation and wish to have it run just as if you were doing so 
from command line.  This mode provides the most flexibility for your liquibase configuration and execution, but provides fewer 
reporting features.

Use "Manage Jenkins" to add your liquibase installation to Jenkins.  It will then be available for selection in the
build step.  Your liquibase installation should include the driver for your database (for example, by having the the driver jar in LIQUIBASE_HOME/lib directory).

Each build's console log will contain the stdout/stderr output of liquibase execution.

**Evaluate changesets**

The "Evaluate liquibase changesets" build step runs liquibase internally and doesn't require an existing installation.  In addition,
build summaries include additional reporting features, like a list of changesets executed.
 
However, using this mode contains a few restrictions:
  * You cannot choose what liquibase command to run; only "update" and "updateTestingRollback" are used.
  * You are restricted to one of the database engines available in the build step's dropdown.
 
# The Future
  
I'd like to minimize the difference between "Invoke" and "Execute" and have changesets be reported in both types of
build steps.
 





