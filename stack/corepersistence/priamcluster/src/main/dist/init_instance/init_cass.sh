#!/bin/bash
echo "${HOSTNAME}" > /etc/hostname
echo "127.0.0.1 ${HOSTNAME}" >> /etc/hosts
hostname `cat /etc/hostname`

echo "US/Eastern" > /etc/timezone
dpkg-reconfigure -f noninteractive tzdata

# Build environment for scripts
. /etc/profile.d/aws-credentials.sh
. /etc/profile.d/usergrid-env.sh

# really annoying part
./install_oraclejdk.sh 

# install the easy stuff
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

/etc/init.d/cassandra start

# Deploy Priam 
rm -rf /var/lib/tomcat7/webapps/*
groovy configure_portal_new.groovy > /var/lib/tomcat7/webapps/portal/config.js 
/etc/init.d/tomcat7 restart

groovy tag_instance.groovy
