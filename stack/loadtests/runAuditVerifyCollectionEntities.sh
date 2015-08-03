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
[ "$#" -ge 4 ] || die "At least 4 arguments required, $# provided.  Example is $0 URL RAMP_USERS RAMP_TIME(seconds) AUDIT_UUID_FILENAME [FAILED_UUID_FILENAME]"

URL="$1"
RAMP_USERS="$2"
RAMP_TIME="$3"
AUDIT_UUID_FILENAME="$4"
FAILED_UUID_FILENAME="$5"

shift 5

#Compile everything
mvn compile

#Set the app id to be a date epoch for uniqueness
#APP=$(date +%s)
ADMIN_USER=superuser
ADMIN_PASSWORD=test
ORG=gatling
APP=millionentities
SCENARIO_TYPE=auditVerifyCollectionEntities

AUTH_TYPE=token
TOKEN_TYPE=management

#Execute the test
mvn gatling:execute \
-Dorg=${ORG} \
-Dapp=${APP} \
-Dbaseurl=${URL} \
-DadminUser=${ADMIN_USER}  \
-DadminPassword=${ADMIN_PASSWORD}  \
-DrampUsers=${RAMP_USERS}  \
-DrampTime=${RAMP_TIME}  \
-DscenarioType=${SCENARIO_TYPE} \
-DauthType=${AUTH_TYPE} \
-DtokenType=${TOKEN_TYPE} \
-DauditUuidFilename=${AUDIT_UUID_FILENAME} \
-DfailedUuidFilename=${FAILED_UUID_FILENAME} \
-Dgatling.simulationClass=org.apache.usergrid.simulations.AuditSimulation



#Now move all the reports
#AGGREGATE_DIR="target/aggregate-$(date +%s)"

#mkdir -p ${AGGREGATE_DIR}

#copy to the format of target/aggregate(date)/(simnulationame)-simulation.log
#find target -name "simulation.log" -exec cp {} ${AGGREGATE_DIR}/$(basename $(dirname {} ))-simulation.log  \;

