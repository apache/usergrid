#!/bin/bash -x
check=`grep "DnumUsers" gatling/bin/gatling.sh`
if [[ $check == "" ]]
then 
sed -i.bak 's/JAVA_OPTS="/JAVA_OPTS="-Dthrottle=${GATLING_THROTTLE} -DnumUsers=${GATLING_NUMUSERS} -DrampTime=${GATLING_RAMPTIME} -Dduration=${GATLING_DURATION} -DnumEntities=${GATLING_NUMENTITIES} -Dbaseurl=${GATLING_BASE_URL} -Dorg=${GATLING_ORG} -Dapp=${GATLING_APP} -Dnotifier=${GATLING_NOTIFIER} -Dprovider=${GATLING_PROVIDER} /g' gatling/bin/gatling.sh
fi
GATLING_NUMUSERS=5000
GATLING_RAMPTIME=300
echo "Enter base url for target server, e.g. http://api.usergrid.com/ (note the trailing slash)"
read GATLING_BASE_URL
echo "Enter org name"
read GATLING_ORG
echo "Enter app name"
read GATLING_APP
echo "Running simulation to load 5k users with geolocation data into /users collection. This will take ~5 minutes."
echo -e "2\n\n\n" | gatling/bin/gatling.sh
echo "Finished loading data into /users collection"
echo 'All done! To get started, set these environment variables:

GATLING_BASE_URL - Required. UG base url, e.g. http://api.usergrid.com/.
GATLING_ORG      - Required. UG organization name.
GATLING_APP      - Required. UG application name.

GATLING_NUMUSERS - Number of users in the simulation. Default is 100.
GATLING_DURATION - Duration of the simulation. Default is 300.
GATLING_RAMPTIME - Time period to inject the users over. Default is 0.
GATLING_THROTTLE - Requests per second the simulation to try to reach. Default is 50.

GATLING_NOTIFIER - Name of the notifier to use for PushNotificationSimulation.
GATLING_PROVIDER - Push notification provider that corresponds to the notifier, e.g. apple, google, etc.'