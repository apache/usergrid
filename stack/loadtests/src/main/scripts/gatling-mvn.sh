#!/bin/sh
URL="$1"
ORG="$2"
APP="$3"
NOTIFIER="$4"
USERS="$5"
RAMP="$6"
shift 6
rm -rf usergrid
git clone https://github.com/apache/incubator-usergrid.git usergrid
cd usergrid/stack
git checkout -b two-dot-o origin/two-dot-o
cd loadtests
mvn clean install
mvn gatling:execute -Dthrottle=3000 -Dduration=300 -DnumEntities=5000 -DnumUsers=${USERS} -DrampTime=${RAMP} -Dbaseurl=${URL} -Dorg=${ORG} -Dapp=${APP} -DpushNotifier=${NOTIFIER} -DpushProvider=noop
