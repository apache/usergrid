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


pushd /etc/apt/sources.list.d

# Install and stop ElasticSearch
cat >> elasticsearch.sources.list << EOF
deb http://packages.elasticsearch.org/elasticsearch/1.2/debian stable main
EOF
apt-get update
apt-get --force-yes -y install elasticsearch
/etc/init.d/elasticsearch stop

mkdir -p /mnt/data/elasticsearch
chown elasticsearch /mnt/data/elasticsearch

mkdir -p /mnt/log/elasticsearch
chown elasticsearch /mnt/log/elasticsearch

# Configure ElasticSearch
cd /usr/share/usergrid/scripts

# No need to do this, elasticsearch nodes are also cassandra nodes
#groovy registry_register.groovy elasticsearch
#groovy wait_for_instances.groovy elasticsearch ${CASSANDRA_NUM_SERVERS}

cat >> /etc/default/elasticsearch << EOF
JAVA_HOME=/usr/lib/jvm/jdk1.7.0
EOF

groovy ./configure_elasticsearch.groovy > /etc/elasticsearch/elasticsearch.yml

update-rc.d elasticsearch defaults 95 10

# Go!
/etc/init.d/elasticsearch start

popd
