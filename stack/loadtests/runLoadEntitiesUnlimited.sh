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
#ORG=
#APP=
#AUTH_TYPE=
#TOKEN_TYPE=
#CREATE_ORG=
#CREATE_APP=
#SANDBOX_COLLECTION=
#NUM_ENTITIES= #ignored
#SKIP_SETUP=
#COLLECTION=
#ENTITY_TYPE=
#ENTITY_PREFIX=
#ENTITY_SEED=  #may be overridden on command line
#RETRY_COUNT=
#ENTITY_PROGRESS_COUNT=
#CONSTANT_USERS_PER_SEC=
#CONSTANT_USERS_DURATION=
#FLUSH_CSV

die() { echo "$@" 1>&2 ; exit 1; }

[ "$#" -ge 2 ] || die "At least 2 arguments required, $# provided.  Example is $0 RAMP_USERS RAMP_TIME(seconds) [UUID_FILENAME [ENTITY_SEED [ENTITY_WORKER_NUM [ENTITY_WORKER_COUNT [USERGRID_REGION]]]]]"

RAMP_USERS="$1"
RAMP_TIME="$2"
[ "$#" -ge 3 ] && UUID_FILENAME="$3"
[ "$#" -ge 4 ] && ENTITY_SEED="$4"
[ "$#" -ge 5 ] && ENTITY_WORKER_NUM="$5"
[ "$#" -ge 6 ] && ENTITY_WORKER_COUNT="$6"
[ "$#" -ge 7 ] && USERGRID_REGION="$6"

shift $#

SCENARIO_TYPE=loadEntities
NEW_CSV_ON_FLUSH=true
FLUSH_CSV=10000

# don't load entities as part of setup (loading entities is the point of the test)
LOAD_ENTITIES=false

#Compile everything
mvn compile

#Execute the test
mvn gatling:execute \
-DflushCsv=${FLUSH_CSV} \
-DunlimitedFeed=true \
-DnewCsvOnFlush=${NEW_CSV_ON_FLUSH} \
-DbaseUrl=${URL} \
-DadminUser=${ADMIN_USER}  \
-DadminPassword=${ADMIN_PASSWORD}  \
-DentityWorkerNum=${ENTITY_WORKER_NUM} \
-DentityWorkerCount=${ENTITY_WORKER_COUNT} \
-Dorg=${ORG} \
-Dapp=${APP} \
-DauthType=${AUTH_TYPE} \
-DtokenType=${TOKEN_TYPE} \
-DcreateOrg=${CREATE_ORG} \
-DcreateApp=${CREATE_APP} \
-DsandboxCollection=${SANDBOX_COLLECTION} \
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
-DuuidFilename=${UUID_FILENAME} \
-DprintFailedRequests=${PRINT_FAILED_REQUESTS} \
-DusergridRegion=${USERGRID_REGION} \
-Dgatling.simulationClass=org.apache.usergrid.simulations.ConfigurableSimulation

