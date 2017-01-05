Release Instructions
=====================

This plugin uses the Maven jgitflow plugin for release.  The following maven execution performs all the neccessary 
operations:
 
 ```
mvn jgitflow:release-start jgitflow:release-finish

 ```
 
Note that write access to the Jenkins repository is required. 