#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. The ASF licenses this file to You
# under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License. For additional information regarding
# copyright in this work, please see the NOTICE file in the top level
# directory of this distribution.
#

# NOTE: To install oracle jdk, we need to accept licence aggrement, therefore we could not download
#       tar ball from oracle's website. We need to put tar ball elsewhere for example S3 like Dave
#       just did here : https://github.com/apache/incubator-usergrid/blob/two-dot-o/stack/awscluster/src/main/dist/init_instance/install_oraclejdk.sh

# Get JDK
# wget http://download.oracle.com/otn-pub/java/jdk/7u65-b17/jdk-7u65-linux-x64.tar.gz

# Install it as they do here:
# http://askubuntu.com/questions/56104/how-can-i-install-sun-oracles-proprietary-java-6-7-jre-or-jdk
# tar -xf jdk-7u65-linux-x64.tar.gz
# mkdir -p /usr/lib/jvm
# mv ./jdk1.7.0_65 /usr/lib/jvm/jdk1.7.0
#
# update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/jdk1.7.0/bin/java" 2000
# update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/jdk1.7.0/bin/javac" 2000
# update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/lib/jvm/jdk1.7.0/bin/javaws" 2000
#
# chmod a+x /usr/bin/java
# chmod a+x /usr/bin/javac
# chmod a+x /usr/bin/javaws
# chown -R root:root /usr/lib/jvm/jdk1.7.0
#
# sudo rm /usr/lib/jvm/default-java
# sudo ln -s /usr/lib/jvm/jdk1.7.0 /usr/lib/jvm/default-java
# sudo apt-get update >> /dev/null
# sudo apt-get --yes --force-yes install openjdk-7-jdk >> /dev/null

echo "Test script started running..."
echo -e "Test script is run successfully, $TEST_PARAM" > /home/ubuntu/runnerOut.log
