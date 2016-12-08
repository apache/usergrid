#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#               http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

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
-DrampTime=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.PostUsersSimulation \
-Dapp=${APP1}


#Execute the get users by username
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DrampTime=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.GetUsersSimulation \
-Dapp=${APP1}


#Execute the get users by page
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DrampTime=${RAMP_TIME}  \
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
-DrampTime=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.PutUsersSimulation \
-Dapp=${APP2}

#Execute the put users to update them
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-rampTime=${RAMP_TIME}  \
-DadminUser=usergrid  \
-DadminPassword=test  \
-Dduration=${DURATION_TIME}    \
-Dgatling.simulationClass=org.apache.usergrid.simulations.PutUsersSimulation \
-Dapp=${APP2}


#Execute the delete to remove them
mvn gatling:execute -Dorg=usergrid \
-Dbaseurl=${URL} \
-DmaxPossibleUsers=${MAX_CONCURRENT_USERS}  \
-DrampTime=${RAMP_TIME}  \
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

