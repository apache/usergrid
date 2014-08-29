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


# Optional, install yourkit remote profiler

if [[ $YOURKIT = "true" ]]; then

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
