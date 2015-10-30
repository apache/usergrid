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
#ORG=
#APP=
#AUTH_TYPE=
#TOKEN_TYPE=
#RETRY_COUNT=

DELETE_AFTER_SUCCESSFUL_AUDIT=false

die() { echo "$@" 1>&2 ; exit 1; }

[ "$#" -ge 3 ] || die "At least 3 arguments required, $# provided.  Example is $0 RAMP_USERS RAMP_TIME(seconds) AUDIT_UUID_FILENAME [FAILED_UUID_FILENAME [DELETE_AFTER_SUCCESSFUL_AUDIT(true/false)[USERGRID_REGION]]]"

RAMP_USERS="$1"
RAMP_TIME="$2"
AUDIT_UUID_FILENAME="$3"
FAILED_UUID_FILENAME="$4"
[ "$#" -ge 5 ] && DELETE_AFTER_SUCCESSFUL_AUDIT="$5"
[ "$#" -ge 6 ] && USERGRID_REGION="$6"

shift $#

SCENARIO_TYPE=auditVerifyCollectionEntities

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
-DretryCount=${RETRY_COUNT} \
-DscenarioType=${SCENARIO_TYPE} \
-DrampUsers=${RAMP_USERS}  \
-DrampTime=${RAMP_TIME}  \
-DauditUuidFilename=${AUDIT_UUID_FILENAME} \
-DfailedUuidFilename=${FAILED_UUID_FILENAME} \
-DprintFailedRequests=${PRINT_FAILED_REQUESTS} \
-DdeleteAfterSuccessfulAudit=${DELETE_AFTER_SUCCESSFUL_AUDIT} \
-DusergridRegion=${USERGRID_REGION} \
-Dgatling.simulationClass=org.apache.usergrid.simulations.AuditSimulation

