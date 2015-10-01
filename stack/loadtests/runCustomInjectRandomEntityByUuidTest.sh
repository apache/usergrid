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

#
# Injection list:
# type(arg1,arg2,...);type(arg1,...)
#
# types:
# rampUsers(int numUsers, int overSeconds)
# constantUsersPerSec(double numUsersPerSec, int duringSeconds)
# constantUsersPerSecRandomized(double numUsersPerSec, int duringSeconds)
# atOnceUsers(int numUsers)
# rampUsersPerSec(double numUsersPerSec, int totalUsers, int duringSeconds)
# rampUsersPerSecRandomized(double numUsersPerSec, int totalUsers, int duringSeconds)
# heavisideUsers(int numUsers, int overSeconds)
# nothingFor(int seconds)
#
# Example: rampUsers(30,120);nothingFor(120);atOnceUsers(20)

DIR="${BASH_SOURCE%/*}"
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. "$DIR/testConfig.sh"

# from testConfig.sh
#URL=
#ADMIN_USER=
#ADMIN_PASSWORD=
#ORG=
#APP=
#AUTH_TYPE=
#TOKEN_TYPE=
#CREATE_ORG=
#CREATE_APP=
#LOAD_ENTITIES=
#SANDBOX_COLLECTION=
#NUM_ENTITIES=
#SKIP_SETUP=
#COLLECTION=
#RETRY_COUNT=
#END_CONDITION_TYPE=
#END_MINUTES=
#END_REQUEST_COUNT=
#INJECTION_LIST=

helpMsg() {
    echo "At least 2 arguments required, $# provided.  Example is $0 INJECTION_LIST UUID_FILENAME" 1>&2
    echo "Injection types:" 1>&2
    echo "  rampUsers(int numUsers, int overSeconds)" 1>&2
    echo "  constantUsersPerSec(double numUsersPerSec, int duringSeconds)" 1>&2
    echo "  constantUsersPerSecRandomized(double numUsersPerSec, int duringSeconds)" 1>&2
    echo "  atOnceUsers(int numUsers)" 1>&2
    echo "  rampUsersPerSec(double numUsersPerSec, int totalUsers, int duringSeconds)" 1>&2
    echo "  rampUsersPerSecRandomized(double numUsersPerSec, int totalUsers, int duringSeconds)" 1>&2
    echo "  heavisideUsers(int numUsers, int overSeconds)" 1>&2
    echo "  nothingFor(int seconds)" 1>&2
    exit 1
}

[ "$#" -ge 2 ] || helpMsg

INJECTION_LIST="$1"
UUID_FILENAME="$2"

shift 2

SCENARIO_TYPE=uuidRandomInfinite

#Compile everything
mvn compile

#Execute the test
mvn gatling:execute \
-DbaseUrl=${URL} \
-DadminUser=${ADMIN_USER}  \
-DadminPassword=${ADMIN_PASSWORD}  \
-Dorg=${ORG} \
-Dapp=${APP} \
-DauthType=${AUTH_TYPE} \
-DtokenType=${TOKEN_TYPE} \
-DcreateOrg=${CREATE_ORG} \
-DcreateApp=${CREATE_APP} \
-DloadEntities=${LOAD_ENTITIES} \
-DsandboxCollection=${SANDBOX_COLLECTION} \
-DnumEntities=${NUM_ENTITIES} \
-DskipSetup=${SKIP_SETUP} \
-Dcollection=${COLLECTION} \
-DretryCount=${RETRY_COUNT} \
-DendConditionType=${END_CONDITION_TYPE} \
-DendMinutes=${END_MINUTES} \
-DendRequestCount=${END_REQUEST_COUNT} \
-DscenarioType=${SCENARIO_TYPE} \
-DuuidFilename=${UUID_FILENAME} \
-DinjectionList=${INJECTION_LIST} \
-DprintFailedRequests=${PRINT_FAILED_REQUESTS} \
-Dgatling.simulationClass=org.apache.usergrid.simulations.CustomInjectionSimulation

