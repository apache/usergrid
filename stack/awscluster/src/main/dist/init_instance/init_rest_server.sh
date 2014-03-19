#!/bin/bash
echo "${HOSTNAME}" > /etc/hostname
echo "127.0.0.1 ${HOSTNAME}" >> /etc/hosts
hostname `cat /etc/hostname`

echo "US/Eastern" > /etc/timezone
dpkg-reconfigure -f noninteractive tzdata

PKGS="openjdk-7-jdk tomcat7 s3cmd ntp unzip groovy"
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

# Wait for enough Cassandra nodes then deploy and restart Tomcat 
cd /usr/share/usergrid/scripts
groovy wait_for_cassandra.groovy

mkdir -p /usr/share/tomcat7/lib 
groovy configure_usergrid.groovy > /usr/share/tomcat7/lib/usergrid-deployment.properties 

rm -rf /var/lib/tomcat7/webapps/*
cp -r /usr/share/usergrid/webapps/* /var/lib/tomcat7/webapps
groovy configure_portal_new.groovy > /var/lib/tomcat7/webapps/portal/config.js 

# Go
/etc/init.d/tomcat7 restart
groovy tag_instance.groovy
