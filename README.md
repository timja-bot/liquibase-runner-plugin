# Jenkins Liquibase Runner Plugin
---

# About

Adds liquibase changeset evaluation as an available build step.  See Liquibase documentation at http://www.liquibase.org/.
Any evaluated changesets are listed on the build's summary page, as well as details about each changeset.

Uses liquibase version 3.4.2.

# Installation

*  Install the liquibase-runner plugin using Jenkins' plugin manager.  Depending on how you use and configure the plugin,
you may need to install a database server, and potentially your target schema.

* Once installed, the "Evaluate liquibase changesets" is made available.

# Configuration


## Simplest

You need only to define the location of your changeset file for the simplest configuration.  Liquibase's
"update" will run using an [H2](http://www.h2database.com) in-memory database.  Changes aren't persisted across builds, so 
each changeset will be listed in each build summary.

## Advanced

Those who would like more control over liquibase exexcution may do so using options presented when using the "advanced"
section of the builder configuration.  Here you'll find most of liquibase's configuration exposed, including contexts and 
the JDBC URL used to access the database.

For convenience, the plugin includes a few database drivers for use here.  Alternatively, you may
also define the classpath where a database driver may be loaded.

# Usage Tips

* If you'd like to have only new changesets evaluated, consider using an H2 JDBC url like 
"jdbc:h2:file:./data/sample".  This instructs H2 to persist the database to a file.  Note, however, if a different
  build slave runs your project, that file will no longer be available, and all changesets will again be executed.
  
* Advance options and simple configuration are not mutually exclusive; you may, for example, select "Test Rollbacks"
  in the advance section and still use the in-memory H2 database. 
