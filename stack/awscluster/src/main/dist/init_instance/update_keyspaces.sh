#!/bin/bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  The ASF licenses this file to You
#  under the Apache License, Version 2.0 (the "License"); you may not
#  use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.  For additional information regarding
#  copyright in this work, please see the NOTICE file in the top level
#  directory of this distribution.
#

#Install keyspaces
# Build environment for scripts
. /etc/profile.d/aws-credentials.sh
. /etc/profile.d/usergrid-env.sh

pushd /etc/apt/sources.list.d

#Run the cassandra cql to create the keyspaces.  Note this only works for the
# us-east region for the replication factor on the keyspaces


#Install cassandra so we have the cli
curl -L http://debian.datastax.com/debian/repo_key | apt-key add -

sudo cat >> /etc/apt/sources.list.d/cassandra.sources.list << EOF
deb http://debian.datastax.com/community stable main
EOF

apt-get update
apt-get -y --force-yes install libcap2 cassandra=1.2.19
/etc/init.d/cassandra stop

#Get the first instance of cassandra
cd /usr/share/usergrid/scripts
CASSHOST=$(groovy get_first_instance.groovy cassandra)

#We have to wait for cass to actually start before we can run our CQL.   Sleep 5 seconds between retries
while ! echo exit | nc ${CASSHOST} 9160; do sleep 5; done

#WE have to remove our -1 from the end, since us-east and us-west dont support -1 in cassandra
CASS_REGION=${EC2_REGION%-1}

#Update the keyspace replication and run the cql
sed -i.bak "s/KEYSPACE_REGION/${CASS_REGION}/g" /usr/share/usergrid/cql/update_locks.cql

sed -i.bak "s/REPLICATION_FACTOR/${CASSANDRA_REPLICATION_FACTOR}/g" /usr/share/usergrid/cql/update_locks.cql


/usr/bin/cassandra-cli -h ${CASSHOST} -f  /usr/share/usergrid/cql/update_locks.cql


#Update the keyspace region and run the cql
sed -i.bak "s/KEYSPACE_REGION/${CASS_REGION}/g" /usr/share/usergrid/cql/update_usergrid.cql
sed -i.bak "s/REPLICATION_FACTOR/${CASSANDRA_REPLICATION_FACTOR}/g" /usr/share/usergrid/cql/update_usergrid.cql

/usr/bin/cassandra-cli -h ${CASSHOST} -f  /usr/share/usergrid/cql/update_usergrid.cql


#Update the keyspace region and run the cql
sed -i.bak "s/KEYSPACE_REGION/${CASS_REGION}/g" /usr/share/usergrid/cql/update_usergrid_applications.cql
sed -i.bak "s/REPLICATION_FACTOR/${CASSANDRA_REPLICATION_FACTOR}/g" /usr/share/usergrid/cql/update_usergrid_applications.cql

/usr/bin/cassandra-cli -h ${CASSHOST} -f  /usr/share/usergrid/cql/update_usergrid_applications.cql


popd
