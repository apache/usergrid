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


# Install and stop Cassandra
curl -L http://debian.datastax.com/debian/repo_key | apt-key add -

sudo cat >> /etc/apt/sources.list.d/cassandra.sources.list << EOF
deb http://debian.datastax.com/community stable main
EOF

apt-get update
apt-get -y --force-yes install libcap2 cassandra=1.2.19
/etc/init.d/cassandra stop

mkdir -p /mnt/data/cassandra
chown cassandra /mnt/data/cassandra

# Wait for other instances to start up
cd /usr/share/usergrid/scripts
groovy registry_register.groovy cassandra
groovy wait_for_instances.groovy cassandra ${CASSANDRA_NUM_SERVERS}

#TODO make this configurable for the box sizes
#Set or min/max heap to 8GB
sed -i.bak s/calculate_heap_sizes\(\)/MAX_HEAP_SIZE=\"8G\"\\nHEAP_NEWSIZE=\"1200M\"\\n\\ncalculate_heap_sizes\(\)/g /etc/cassandra/cassandra-env.sh

pushd /usr/share/usergrid/scripts
groovy configure_cassandra.groovy > /etc/cassandra/cassandra.yaml
popd

/etc/init.d/cassandra start


