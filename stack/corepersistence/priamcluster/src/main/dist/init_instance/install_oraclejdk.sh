#!/bin/bash
pushd /tmp

# Get JDK from the release bucket
s3cmd --config=/etc/s3cfg get s3://${RELEASE_BUCKET}/jdk-7u45-linux-x64.gz

# Install it as they do here: 
# http://askubuntu.com/questions/56104/how-can-i-install-sun-oracles-proprietary-java-6-7-jre-or-jdk
tar -xvf jdk-7u2-linux-x64.tar.gz
mkdir -p /usr/lib/jvm
mv ./jdk1.7.0_02 /usr/lib/jvm/jdk1.7.0

update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/jdk1.7.0/bin/java" 1
update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/jdk1.7.0/bin/javac" 1
update-alternatives --install "/usr/bin/javaws" "javaws" "/usr/lib/jvm/jdk1.7.0/bin/javaws" 1

chmod a+x /usr/bin/java 
chmod a+x /usr/bin/javac 
chmod a+x /usr/bin/javaws
chown -R root:root /usr/lib/jvm/jdk1.7.0

popd