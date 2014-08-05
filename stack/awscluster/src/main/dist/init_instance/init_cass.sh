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

echo "${HOSTNAME}" > /etc/hostname
echo "127.0.0.1 ${HOSTNAME}" >> /etc/hosts
hostname `cat /etc/hostname`

echo "US/Eastern" > /etc/timezone
dpkg-reconfigure -f noninteractive tzdata

PKGS="openjdk-7-jdk s3cmd ntp unzip groovy"
apt-get update
apt-get -y install ${PKGS}

# Install AWS Java SDK and get it into the Groovy classpath
curl http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip > /tmp/aws-sdk-java.zip
cd /usr/share/
unzip /tmp/aws-sdk-java.zip 
mkdir -p /home/ubuntu/.groovy/lib
cp /usr/share/aws-java-sdk-*/third-party/*/*.jar /home/ubuntu/.groovy/lib
cp /usr/share/aws-java-sdk-*/lib/* /home/ubuntu/.groovy/lib 
# except for evil stax
rm /home/ubuntu/.groovy/lib/stax*
ln -s /home/ubuntu/.groovy /root/.groovy

# Build environment for Groovy scripts
. /etc/profile.d/aws-credentials.sh
. /etc/profile.d/usergrid-env.sh



# Register as a Cassandra node and wait for enough other servers to join
cd /usr/share/usergrid/scripts

groovy registry_register.groovy
groovy wait_for_cassandra.groovy

# Install and stop Cassandra so we can configure it
cd /etc/apt/sources.list.d
cat >> cassandra.sources.list << EOF
deb http://www.apache.org/dist/cassandra/debian 12x main
EOF
sudo apt-get update
sudo apt-get -y install libcap2
sudo apt-get --force-yes -y install cassandra
/etc/init.d/cassandra stop

cd /usr/share/usergrid/init_instance
cd /usr/share/usergrid/scripts
groovy configure_cassandra.groovy > /etc/cassandra/cassandra.yaml

# Go
/etc/init.d/cassandra start

