
#####
# Optional, install yourkit remote profiler
#####

if [ $YOURKIT = "true" ]; then

mkdir -p /tmp/yourkit
cd /tmp/yourkit
s3cmd --config=/etc/s3cfg get s3://${RELEASE_BUCKET}/yjp-2013-build-13088.zip
unzip /tmp/yourkit/yjp-2013-build-13088.zip

mkdir -p /tmp/yourkitreports

chown -R tomcat7.tomcat7 /tmp/yourkitreports

cat >> /etc/default/tomcat7 << EOF
JAVA_OPTS="${JAVA_OPTS} -agentpath:/tmp/yourkit/yjp-2013-build-13088/bin/linux-x86-64/libyjpagent.so=port=10001,logdir=/tmp/yourkitreports,onexit=snapshot"
EOF

fi

######
# End yourkit installation
######
