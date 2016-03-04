# Jenkins Liquibase Runner Plugin
---

# About

Adds Liquibase as an available build step.  See Liquibase documentation at http://www.liquibase.org/.

# Installation

*  Install the liquibase-runner plugin using Jenkins' plugin manager.  Depending on how you use and configure the plugin,
you may need to install a database server, and potentially your target schema.

# Configuration

**Evaluate changesets**

Once installed, a new build step is available, "Evaluate liquibase changesets".  Just like it says, once you've
defined the path to your root changeset file, this step will run liquibase update against it.

Once a build has complete, a list of which changesets were executed are listed on the build's page.  When possible,
the generated SQL for each changeset is provided as well.
 
 
# Usage Tips

* If you're using the H2 database engine, you can use JDBC url like "jdbc:h2:mem:test" to create a in-memory database
with each execution.  If you'd like to have the schema persist between builds, use a jdbc url like 
"jdbc:h2:file:./data/sample", so that the schema is persisted on disk.  No username/password needed. 

* If you'd like to use a database engine like MySQL or postgres, you'll have to ensure the schema exists and is 
accessible to Jenkins.  Consider having your Jenkins job execute an initialize script to do so.


 





