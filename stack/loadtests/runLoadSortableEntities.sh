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
[ "$#" -ge 8 ] || die "At least 8 arguments required, $# provided.  Example is $0 URL RAMP_USERS RAMP_TIME(seconds) CONSTANT_USERS_PER_SEC, CONSTANT_USERS_DURATION(seconds) NUM_ENTITIES ENTITY_WORKER_NUM ENTITY_WORKER_COUNT [UUID_FILENAME]"

URL="$1"
RAMP_USERS="$2"
RAMP_TIME="$3"
CONSTANT_USERS_PER_SEC="$4"
CONSTANT_USERS_DURATION="$5"
NUM_ENTITIES="$6"
ENTITY_WORKER_NUM="$7"
ENTITY_WORKER_COUNT="$8"
UUID_FILENAME="$9"

shift 9

#Compile everything
mvn compile

#Set the app id to be a date epoch for uniqueness
#APP=$(date +%s)
ADMIN_USER=superuser
ADMIN_PASSWORD=test
CREATE_ORG=false
ORG=gatling
CREATE_APP=false
APP=millionentities
COLLECTION=sortableentities
SANDBOX_COLLECTION=true
SCENARIO_TYPE=loadEntities
# don't load entities as part of setup
LOAD_ENTITIES=false

SKIP_SETUP=false
#SEARCH_QUERY=order%20by%20specials%20desc
#SEARCH_LIMIT=1000
ENTITY_TYPE=trivialSortable
ENTITY_PREFIX=sortable
ENTITY_SEED=1
AUTH_TYPE=token
TOKEN_TYPE=management
END_CONDITION_TYPE=minutesElapsed
#END_CONDITION_TYPE=requestCount
END_MINUTES=2
END_REQUEST_COUNT=100

#Execute the test
mvn gatling:execute \
-DskipSetup=${SKIP_SETUP} \
-DcreateOrg=${CREATE_ORG} \
-Dorg=${ORG} \
-DcreateApp=${CREATE_APP} \
-Dapp=${APP} \
-Dbaseurl=${URL} \
-DadminUser=${ADMIN_USER}  \
-DadminPassword=${ADMIN_PASSWORD}  \
-DloadEntities=${LOAD_ENTITIES} \
-DnumEntities=${NUM_ENTITIES} \
-DentityType=${ENTITY_TYPE} \
-DentityPrefix=${ENTITY_PREFIX} \
-DentitySeed=${ENTITY_SEED} \
-DrampUsers=${RAMP_USERS}  \
-DrampTime=${RAMP_TIME}  \
-DconstantUsersPerSec=${CONSTANT_USERS_PER_SEC}    \
-DconstantUsersDuration=${CONSTANT_USERS_DURATION}    \
-Dcollection=${COLLECTION} \
-DscenarioType=${SCENARIO_TYPE} \
-DauthType=${AUTH_TYPE} \
-DtokenType=${TOKEN_TYPE} \
-DendConditionType=${END_CONDITION_TYPE} \
-DendMinutes=${END_MINUTES} \
-DendRequestCount=${END_REQUEST_COUNT} \
-DentityWorkerCount=${ENTITY_WORKER_COUNT} \
-DentityWorkerNum=${ENTITY_WORKER_NUM} \
-DuuidFilename=${UUID_FILENAME} \
-DsandboxCollection=${SANDBOX_COLLECTION} \
-Dgatling.simulationClass=org.apache.usergrid.simulations.ConfigurableSimulation



#Now move all the reports
#AGGREGATE_DIR="target/aggregate-$(date +%s)"

#mkdir -p ${AGGREGATE_DIR}

#copy to the format of target/aggregate(date)/(simnulationame)-simulation.log
#find target -name "simulation.log" -exec cp {} ${AGGREGATE_DIR}/$(basename $(dirname {} ))-simulation.log  \;

