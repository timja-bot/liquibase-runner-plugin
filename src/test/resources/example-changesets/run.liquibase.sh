#!/bin/bash

# NOTE: this script is expected to run in the same directory that h2.liquibase.properties resides

changeset=${1:-"include-all-relative.xml"}

liquibase --defaultsFile=h2.liquibase.properties --changeLogFile="${changeset}" update
