#!/bin/bash
echo "${HOSTNAME}" > /etc/hostname
echo "127.0.0.1 ${HOSTNAME}" >> /etc/hosts
hostname `cat /etc/hostname`

echo "US/Eastern" > /etc/timezone
dpkg-reconfigure -f noninteractive tzdata

# Build environment for scripts
. /etc/profile.d/aws-credentials.sh
. /etc/profile.d/usergrid-env.sh

# Install Oracle JDK from our S3 bucket
./install_oraclejdk.sh 

# Install the easy stuff
PKGS="ntp unzip groovy tomcat7"
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

# Install and stop Cassandra
cd /etc/apt/sources.list.d
cat >> cassandra.sources.list << EOF
deb http://www.apache.org/dist/cassandra/debian 12x main
EOF
sudo apt-get update
sudo apt-get -y install libcap2
sudo apt-get --force-yes -y install cassandra
/etc/init.d/cassandra stop
rm -rf /var/log/cassandra/*

# Provide initial configuration to Cassandra 
cd /usr/share/usergrid/scripts
groovy registry_register.groovy
groovy wait_for_cassandra.groovy
cd /usr/share/usergrid/init_instance
cd /usr/share/usergrid/scripts
groovy configure_cassandra.groovy > /etc/cassandra/cassandra.yaml

# Configure Priam
cd /usr/share/usergrid/scripts
groovy configure_priam.groovy

# Copy Priam extension into Cassandra and Priam WAR into Tomcat
rm -rf /var/lib/tomcat7/webapps/*
mkdir -p /usr/share/cassandra/lib 
cp /usr/share/usergrid/lib/priam-cass-extensions-2.0.0-SNAPSHOT.jar /usr/share/cassandra/lib 
cp /usr/share/usergrid/webapps/priam-web-2.0.0-SNAPSHOT.war /var/lib/tomcat7/webapps/Priam.war

# Make sure Priam via Tomcat can write to /etc/cassandra
chmod -R 777 /etc/cassandra
chmod 777 /etc/init.d/cassandra
# TODO: need to do something like this instead (not sure why it does not work):
#chmod 770 /etc/cassandra
#chmod -R 660 /etc/cassandra/*
#usermod -a -G cassandra tomcat7
#chgrp -R cassandra /etc/cassandra

# Start Priam via Tomcat, should cause Cassandra to start
/etc/init.d/tomcat7 restart

groovy tag_instance.groovy
