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
groovy registry_register.groovy cassandra
groovy wait_for_instances.groovy cassandra

cd /usr/share/usergrid/scripts
groovy configure_cassandra.groovy > /etc/cassandra/cassandra.yaml

/etc/init.d/cassandra start

#Install opscenter
echo "deb http://debian.datastax.com/community stable main" | sudo tee -a /etc/apt/sources.list.d/datastax.community.list

curl -L http://debian.datastax.com/debian/repo_key | apt-key add -

apt-get update
apt-get  --force-yes -y install opscenter

sudo service opscenterd start





## Configure Priam
#groovy configure_priam.groovy
#
## Copy Priam extension into Cassandra and Priam WAR into Tomcat
#rm -rf /var/lib/tomcat7/webapps/*
#mkdir -p /usr/share/cassandra/lib 
#cp /usr/share/usergrid/lib/priam-cass-extensions-1.2.24.jar /usr/share/cassandra/lib 
#cp /usr/share/usergrid/webapps/priam-web-1.2.24.war /var/lib/tomcat7/webapps/Priam.war
#
## Make sure Priam via Tomcat can write to /etc/cassandra
## TODO: do this without 777
#chmod -R 777 /etc/cassandra
#chmod 777 /etc/init.d/cassandra
#
## Configure sudo for no passwords to please Priam
#cat >> /tmp/sudoers.new << EOF
#Defaults    env_reset
#Defaults    mail_badpass
#Defaults    secure_path="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
#root    ALL=(ALL:ALL) ALL
#%admin  ALL=(ALL) ALL
#%sudo   ALL=(ALL:ALL) NOPASSWD:ALL
#EOF
#visudo -c -f /tmp/sudoers.new
#if [ "$?" -eq "0" ]; then
#    cp /tmp/sudoers.new /etc/sudoers
#fi
## Add tomat user to sudoers to please Priam
#adduser tomcat7 sudo
#/etc/init.d/sudo restart

popd

