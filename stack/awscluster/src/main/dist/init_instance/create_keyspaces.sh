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
pushd /etc/apt/sources.list.d

#Run the cassandra cql to create the keyspaces.  Note this only works for the
# us-east region for the replication factor on the keyspaces

cd /usr/share/usergrid/scripts
FIRSTHOST="$(groovy get_first_instance.groovy cassandra)"

if [ "$FIRSTHOST"=="$PUBLIC_HOSTNAME" ]; then


#Update the keyspace replication and run the cql
sed -i.bak "s/KEYSPACE_REGION/${EC2_REGION}/g" /usr/share/usergrid/cql/create_locks.cql

/usr/bin/cassandra-cli -f  /usr/share/usergrid/cql/create_locks.cql




#Update the keyspace region and run the cql
sed -i.bak "s/KEYSPACE_REGION/${EC2_REGION}/g" /usr/share/usergrid/cql/create_usergrid.cql

/usr/bin/cassandra-cli -f  /usr/share/usergrid/cql/create_usergrid.cql




#Update the keyspace region and run the cql
sed -i.bak "s/KEYSPACE_REGION/${EC2_REGION}/g" /usr/share/usergrid/cql/create_usergrid_applications.cql

/usr/bin/cassandra-cli -f  /usr/share/usergrid/cql/create_usergrid_applications.cql

fi

popd
