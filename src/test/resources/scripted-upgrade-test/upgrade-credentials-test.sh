#!/bin/bash

# scripted test for covering plugin upgrade from 1.1.0 to 1.2.0, which has credentials migration logic.
# Maybe possible to use JenkinsRule in unit test for this, but I just couldn't find how.

# Some notes:
#   - Locally I was using jenkins 2.32.1
#   - due to cli auth errors, copies an unsecured jenkins config file to jenkins home.
#   - intended to be run in directory which script lives
#   - assumes jenkins home existing at "$HOME/.jenkins"
#   - assumes jenkins has been started and initialized at least once prior to script execution.
#   - This is a pretty hacky script!  Nevertheless I wanted to have some test regarding upgrade.

war_file=""
cli_jar=""
job_config_file="./oldauth_job_config.xml"
changelog_filepath=".././example-changesets/sunny-day-changeset.xml"
hpi_path="../../../../target/liquibase-runner.hpi"

jenkins_config_file="./unsecured-jenkins.xml"
jenkins_home="$HOME/.jenkins"

while getopts "w:f:c:l:j:" options; do
   case $options in
      w ) war_file="$OPTARG";;
      f ) hpi_path="$OPTARG";;
      c ) cli_jar="$OPTARG";;
      l ) changelog_filepath="$OPTARG";;
      j ) job_config_file="$OPTARG";;
   esac
done

if [ ! -e "$war_file" ] || [ ! -e "$cli_jar" ]; then
   echo "You must supply war file path (-w) and cli file path (-c)"
   exit 1
fi

backup_made="false"
jenkins_config_path="$jenkins_home/config.xml"
changelog_filepath=`readlink -e "$changelog_filepath"`
base_path=`dirname "$changelog_filepath"`
changelog_filename=`basename "$changelog_filepath"`
job_name=`cat /dev/urandom | tr -cd 'a-f0-9' | head -c 10`
h2_file_path=`mktemp`

current_dir=`pwd`
clone_dir=`mktemp -d`

cd /tmp &&
echo "checking out and building 1.1.0 version of plugin"

git clone git@github.com:jenkinsci/liquibase-runner-plugin.git "$clone_dir" &&
cd "$clone_dir" &&

git checkout 1.1.0 &&
mvn -Dmaven.test.skip=true package ;
cd "$current_dir"


echo "building plugin"
cd ../../../.. && mvn -Dmaven.test.skip=true package && cd -

echo "removing existing plugins"
rm -rf "$jenkins_home/plugins/"*

if [ -e "$jenkins_config_path" ]; then
  echo "making backup of existing jenkins config"
  backup_made="true"
  cp "$jenkins_config_path" "$jenkins_home/config.xml.bak"
fi

echo "copying unsecured jenkins config"
cp "$jenkins_config_file" "$jenkins_home/config.xml"

echo "starting jenkins."
java -jar "$war_file" </dev/null &>/dev/null &
sleep 10       &&


echo "installing plugins"

java -jar "$cli_jar" -noKeyAuth install-plugin "$clone_dir/target/liquibase-runner.hpi"  </dev/null &>/dev/null &
sleep 3     &&

java -jar "$cli_jar" -noKeyAuth install-plugin "credentials"     </dev/null &>/dev/null &
sleep 3     &&
java -jar "$cli_jar" -noKeyAuth install-plugin "workflow-step-api"   </dev/null &>/dev/null &
sleep 3     &&

echo "restarting jenkins"
java -jar "$cli_jar" -noKeyAuth restart    &&

sleep 15 &&

echo "creating job '$job_name'"
cat "$job_config_file" | sed -e 's|@H2_FILE@|'"$h2_file_path"'|g' -e 's|@CHANGELOG_FILEPATH@|'"$changelog_filepath"'|g' -e 's|@BASE_PATH@|'"$base_path"'|g' | java -jar "$cli_jar" -noKeyAuth create-job "$job_name"  &&


sleep 3   &&

echo "running pre-upgraded $job_name"
java -jar "$cli_jar" -noKeyAuth build "$job_name" -f | grep "SUCCESS" &&


echo "installing plugin from file"
java -jar "$cli_jar" -noKeyAuth install-plugin "$hpi_path"   &

sleep 2 &&

echo "restarting jenkins"
java -jar "$cli_jar" -noKeyAuth restart   &&

sleep 10 &&
echo "building $job_name"
java -jar "$cli_jar" -noKeyAuth build "$job_name" -f | grep "SUCCESS" &&

status="$?"

echo "deleting created job"
java -jar "$cli_jar" -noKeyAuth delete-job "$job_name" &&

echo "killing jenkins process"
jps -lv | grep jenk | grep -oE "^[0-9]+" | xargs kill

if [ -e "$h2_file_path" ]; then
  rm "$h2_file_path"
fi

if [ -e "$clone_dir" ]; then
  rm -rf "$clone_dir"
fi

if [ "$backup_made" == "true" ]; then
    echo "restoring previous jenkins config"
    cp "$jenkins_home/config.xml.bak" "$jenkins_config_path"
fi

cp "$jenkins_config_file" "$jenkins_home/config.xml"
if [ "$status" != 0 ]; then
    echo "build failure"
    exit 1
else
    echo "success"
    exit 0
fi
