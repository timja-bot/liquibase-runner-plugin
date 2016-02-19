# Jenkins Liquibase Plugin
---

# About

Adds Liquibase as an available build step.  See Liquibase documentation at http://www.liquibase.org/.

# Installation

*  Install the liquibase-runner plugin.

# Configuration

## "Execute" versus "Invoke"

This plugin has two modes for running liquibase.  The first evaluates liquibase changesets internally, and requires 
no existing liquibase installation.  The second invokes an existing liquibase installation.  Details about each are given
below.

In each mode, basic liquibase options, such as context and database URL, are provided as configuraiton options in the 
build steps.

**Execute**

Use this build step if you have an existing liquibase installation and wish to have it run just as if you were doing so 
from command line.  This mode provides the most flexibility for your liquibase configuration and execution, but provides fewer 
reporting features.

Once you've created your installation in Jenkins's configuration, it will be available for selection in this build 
step.  Each build's console log will contain the stdout/stderr output of liquibase.

Your liquibase installation should include the driver for your database (for example, by having the the driver jar in LIQUIBASE_HOME/lib directory).

**Invoke**

The "Invoke Liquibase" build step runs liquibase internally and doesn't require an existing installation.  In addition,
build summaries include additional reporting features, like a list of changesets executed.
 
However, using this mode does not allow you to choose what liquibase command to run.  Furthermore, you'll be restricted
to using one of the included database drivers. 
 
# The Future
  
I'd like to minimize the difference between "Invoke" and "Execute" and have changesets be reported in both types of
build steps.
 





