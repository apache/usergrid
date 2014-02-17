#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# NOTE: Please define the right path to your Cassandra Home!.
export UG_CASSANDRA_HOME=/Dev/apache-cassandra-0.8.0-rc1
export CASSANDRA_DATA_DIR=/var/lib/cassandra
export CASSANDRA_LOGS=/var/log/cassandra
export UG_TOOL_HOME=`dirname $0`
export UG_EXPORT_TOOL_JAR=$UG_TOOL_HOME/target/usergrid-tools-0.0.1-SNAPSHOT.jar

echo "usergird-tool home: $UG_TOOL_HOME"

# Use JAVA_HOME if set, otherwise look for java in PATH
if [ -x $JAVA_HOME/bin/java ]; then
    JAVA=$JAVA_HOME/bin/java
else
    JAVA=`which java`
    export JAVA_HOME=$(readlink -f $JAVA | sed "s:bin/java::")
fi

echo "Stoping Cassandra..."
pkill java

# Delete data if "-d" option was set.
if [ "$1" == "-d" ]; then
    echo "deleting data...."
    echo "rm -rf $CASSANDRA_DATA_DIR/*"
    rm -rf $CASSANDRA_DATA_DIR/*
    echo "rm -rf $CASSANDRA_LOGS/*"
    rm -rf $CASSANDRA_LOGS/*
    echo "done"

    setup="yes"
fi

JVM_OPTS=""
JVM_OPTS="$JVM_OPTS -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1415"

echo "Starting up cassandra again"
$UG_CASSANDRA_HOME/bin/cassandra
sleep 5

if [ "$setup" == "yes" ]; then
	echo "Setup DB"
	$JAVA $JVM_OPTS -jar $UG_EXPORT_TOOL_JAR SetupDB
	echo "Populating"
	$JAVA $JVM_OPTS -jar $UG_EXPORT_TOOL_JAR PopulateSample
fi


echo "."
sleep 1
echo "."
echo "Exporting...."
sleep 1

$JAVA $JVM_OPTS -jar $UG_EXPORT_TOOL_JAR Export -outputDir ./target/exportFirst

echo "."
echo "Export Done!"
echo "."
echo "Stopping/dleting/starting."

pkill java
rm -rf $CASSANDRA_DATA_DIR/*
rm -rf $CASSANDRA_LOGS/*
$UG_CASSANDRA_HOME/bin/cassandra
sleep 5
$JAVA $JVM_OPTS -jar $UG_EXPORT_TOOL_JAR SetupDB
sleep 1

echo "."
echo "Importing ...."
echo "."
sleep 1
$JAVA $JVM_OPTS -jar $UG_EXPORT_TOOL_JAR Import -inputDir ./target/exportFirst

