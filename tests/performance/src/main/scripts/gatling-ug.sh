#!/bin/sh
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
die () {
    echo >&2 "$@"
    exit 1
}

[ "$#" -eq 5 ] || die "5 arguments required, $# provided.  Arguments are URL ORG APP NUM_USERS RAMP_TIME"

OLDDIR=`pwd`
BIN_DIR=`dirname $0`
cd "${BIN_DIR}/.." && DEFAULT_GATLING_HOME=`pwd` && cd "${OLDDIR}"

GATLING_HOME="${GATLING_HOME:=${DEFAULT_GATLING_HOME}}"
GATLING_CONF="${GATLING_CONF:=$GATLING_HOME/conf}"
URL="$1"
ORG="$2"
APP="$3"
USERS="$4"
RAMP="$5"

#Shift off our first operation
shift 5

export GATLING_HOME GATLING_CONF

echo "GATLING_HOME is set to ${GATLING_HOME}"

curl -X POST "${URL}/usergrid/sandbox/notifiers" -d '{"name":"notifier82e05787a8c24361a2992c64436b6e6a","provider":"noop"}'

#Add -Ds=<simulation class name>

JAVA_OPTS="-Dthrottle=3000 -Dduration=300 -Dorg=${ORG} -Dbaseurl=${URL} -Dnotifier=notifier82e05787a8c24361a2992c64436b6e6a -DnumEntities=10000 -DnumUsers=${USERS} -DrampTime=${RAMP} -Dapp=${APP} -server -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx512M -Xmn100M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${JAVA_OPTS}"

echo $JAVA_OPTS

CLASSPATH="$GATLING_HOME/lib/*:$GATLING_CONF:$GATLING_HOME/user-files:${JAVA_CLASSPATH}"

java $JAVA_OPTS -cp "$CLASSPATH" io.gatling.app.Gatling "$@"
