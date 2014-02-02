#!/bin/bash
echo "${HOSTNAME}" > /etc/hostname
echo "127.0.0.1 ${HOSTNAME}" >> /etc/hosts
hostname `cat /etc/hostname`

echo "US/Eastern" > /etc/timezone
dpkg-reconfigure -f noninteractive tzdata

# Build environment for scripts
. /etc/profile.d/aws-credentials.sh
. /etc/profile.d/usergrid-env.sh

./create_raid0.sh

# Install the easy stuff
PKGS="ntp unzip groovy tomcat7 curl"
apt-get update
apt-get -y install ${PKGS}
/etc/init.d/tomcat7 stop

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

./install_oraclejdk.sh 

# Install and stop Cassandra 
cd /usr/share/usergrid/init_instance
./install_cassandra.sh

# Install and start ElasticSearch
cd /usr/share/usergrid/init_instance
./install_elasticsearch.sh
/etc/init.d/elasticsearch start

# Starting Tomcat starts Priam which starts Priam
/etc/init.d/tomcat7 restart

# Priam consistently craps out on first run
# making this ugly kludge necessary
sleep 90
/etc/init.d/tomcat7 restart

cd /usr/share/usergrid/scripts
groovy tag_instance.groovy
