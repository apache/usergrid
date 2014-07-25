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
pushd /etc/apt/sources.list.d

cat >> cassandra.sources.list << EOF
deb http://www.apache.org/dist/cassandra/debian 12x main
EOF
apt-get update
apt-get -y install libcap2
apt-get --force-yes -y install cassandra
/etc/init.d/cassandra stop

mkdir -p /mnt/data/cassandra
chown cassandra /mnt/data/cassandra

# Wait for other instances to start up
cd /usr/share/usergrid/scripts
groovy registry_register.groovy
groovy wait_for_instances.groovy

cd /usr/share/usergrid/scripts
groovy configure_cassandra.groovy > /etc/cassandra/cassandra.yaml

/etc/init.d/cassandra start

# Install opscenter
echo "deb http://debian.datastax.com/community stable main" | sudo tee -a /etc/apt/sources.list.d/datastax.community.list

curl -L http://debian.datastax.com/debian/repo_key | apt-key add -

apt-get update
apt-get  --force-yes -y install opscenter

sudo service opscenterd start

popd

