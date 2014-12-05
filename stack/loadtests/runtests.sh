#!/bin/bash

die() { echo "$@" 1>&2 ; exit 1; }

####
#This is a script to simplify running gatling tests.  It will default several parameters, invoke the maven plugins
#Then aggregate the results
####
[ "$#" -eq 4 ] || die "4 arguments required, $# provided.  Example is $0 URL MAX_CONCURRENT_USERS RAMP_TIME(seconds) DURATION_TIME(seconds)"

URL="$1"
MAX_CONCURRENT_USERS="$2"
RAMP_TIME="$3"
DURATION_TIME="$4"

shift 4

#Compile everything
mvn compile

#Set the app id to be a date epoch for uniqueness
APP1=$(date +%s)



#Execute the post step
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DmaxPossibleUsers=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.PostUsersSimulation \
-Dapp=${APP1}


#Execute the get users by username
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DmaxPossibleUsers=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.GetUsersSimulation \
-Dapp=${APP1}


#Execute the get users by page
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DmaxPossibleUsers=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.GetUserPagesSimulation \
-Dapp=${APP1}


APP2=$(date +%s)

#Execute put users to create them
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DmaxPossibleUsers=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.PutUsersSimulation \
-Dapp=${APP2}

#Execute the put users to update them
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DmaxPossibleUsers=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.PutUsersSimulation \
-Dapp=${APP2}


#Execute the delete to remove them
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DmaxPossibleUsers=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.DeleteUsersSimulation \
-Dapp=${APP2}


#Now move all the reports
#AGGREGATE_DIR="target/aggregate-$(date +%s)"

#mkdir -p ${AGGREGATE_DIR}


#copy to the format of target/aggregate(date)/(simnulationame)-simulation.log
#find target -name "simulation.log" -exec cp {} ${AGGREGATE_DIR}/$(basename $(dirname {} ))-simulation.log  \;

