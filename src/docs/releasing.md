Release Instructions
=====================

This plugin uses the Maven jgitflow plugin for release.  The following maven execution performs all the neccessary 
operations:
 
```
mvn clean jgitflow:release-start jgitflow:release-finish -DdevelopmentVersion=(next snapshot version)
``` 
Note that write access to the Jenkins maven repository is required. 