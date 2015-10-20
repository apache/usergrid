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

####
# Configuration for tests
####

URL="http://localhost:8080"
ADMIN_USER=superuser
ADMIN_PASSWORD=test

ENTITY_WORKER_NUM=1
ENTITY_WORKER_COUNT=1

ORG=gatling
APP=millionentities

AUTH_TYPE=token
TOKEN_TYPE=management

CREATE_ORG=false
CREATE_APP=false
LOAD_ENTITIES=false
SANDBOX_COLLECTION=true
NUM_ENTITIES=10000
SKIP_SETUP=false

COLLECTION=thousands
ENTITY_TYPE=trivialSortable
ENTITY_PREFIX=sortable
ENTITY_SEED=1

SEARCH_QUERY=order%20by%20sortField%20desc
SEARCH_LIMIT=1000
RETRY_COUNT=5
LATER_THAN_TIMESTAMP=0
ENTITY_PROGRESS_COUNT=1000

END_CONDITION_TYPE=minutesElapsed
#END_CONDITION_TYPE=requestCount
END_MINUTES=5
END_REQUEST_COUNT=100

CONSTANT_USERS_PER_SEC=0
CONSTANT_USERS_DURATION=10

INJECTION_LIST="rampUsers(100,300);nothingFor(300)"

PRINT_FAILED_REQUESTS=true

GET_VIA_QUERY=false
QUERY_PARAMS=

FLUSH_CSV=10000
USERGRID_REGION=
