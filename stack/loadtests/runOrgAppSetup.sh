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

DIR="${BASH_SOURCE%/*}"
if [[ ! -d "$DIR" ]]; then DIR="$PWD"; fi
. "$DIR/testConfig.sh"

# from testConfig.sh
#URL=
#ADMIN_USER=
#ADMIN_PASSWORD=
#ENTITY_WORKER_NUM=  #may be overridden on command line
#ENTITY_WORKER_COUNT=  #may be overridden on command line
#AUTH_TYPE=
#TOKEN_TYPE=
#NUM_ENTITIES=  #may be overridden on command line
#ENTITY_TYPE=
#ENTITY_PREFIX=
#ENTITY_SEED=  #may be overridden on command line
#RETRY_COUNT=
#ENTITY_PROGRESS_COUNT=
#CONSTANT_USERS_PER_SEC=
#CONSTANT_USERS_DURATION=

die() { echo "$@" 1>&2 ; exit 1; }

[ "$#" -ge 2 ] || die "At least 2 arguments required, $# provided.  Example is $0 ORG APP [COLLECTION [SANDBOX_COLLECTION (true/false)]]"

ORG="$1"
APP="$2"
COLLECTION="gatlingitems"
[ "$#" -ge 3 ] && COLLECTION="$3"
SANDBOX_COLLECTION=true
[ "$#" -ge 4 ] && SANDBOX_COLLECTION="$4"

shift $#

SCENARIO_TYPE=doNothing
SKIP_SETUP=false
CREATE_ORG=true
CREATE_APP=true
RAMP_USERS=1
RAMP_TIME=1

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
-DsandboxCollection=${SANDBOX_COLLECTION} \
-DnumEntities=${NUM_ENTITIES} \
-DskipSetup=${SKIP_SETUP} \
-Dcollection=${COLLECTION} \
-DentityType=${ENTITY_TYPE} \
-DentityPrefix=${ENTITY_PREFIX} \
-DentitySeed=${ENTITY_SEED} \
-DretryCount=${RETRY_COUNT} \
-DentityProgressCount=${ENTITY_PROGRESS_COUNT} \
-DconstantUsersPerSec=${CONSTANT_USERS_PER_SEC}    \
-DconstantUsersDuration=${CONSTANT_USERS_DURATION}    \
-DscenarioType=${SCENARIO_TYPE} \
-DloadEntities=${LOAD_ENTITIES} \
-DrampUsers=${RAMP_USERS}  \
-DrampTime=${RAMP_TIME}  \
-DprintFailedRequests=${PRINT_FAILED_REQUESTS} \
-Dgatling.simulationClass=org.apache.usergrid.simulations.ConfigurableSimulation

