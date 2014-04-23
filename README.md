Jenkins Liquibase Plugin
=================

About
-----
Adds Liquibase as an available build step.  See Liquibase documentation at http://www.liquibase.org/.

Configuration Guide
----

Add your liquibase installation in Manage Jenkins -> Configure System.  Note that the jar file containing
your database driver should be located in LIQUIBASE_HOME/lib.

Tips for use
-----
* The target database should be created beforehand.
* Liquibase's "updateTestingRollback" is the recommended command to use, as it will execute rollback routines in
addition to pending changesets.
* You can use the ["dbDoc"](http://www.liquibase.org/documentation/dbdoc.html) command to generate Liquibase's
javadoc-like documentation and use the [JavaDoc](https://wiki.jenkins-ci.org/display/JENKINS/Javadoc+Plugin) plugin to publish it.



