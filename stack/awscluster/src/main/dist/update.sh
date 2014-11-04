#!/bin/bash

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

sudo mkdir __tmpupdate__
pushd __tmpupdate__

    sudo s3cmd --config=/etc/s3cfg get s3://${RELEASE_BUCKET}/ROOT.war
    # sudo tar xzvf awscluster-1.0-SNAPSHOT-any.tar.gz
    sudo /etc/init.d/tomcat7 stop
    sudo cp -r ROOT.war /var/lib/tomcat7/webapps

    pushd /usr/share/usergrid/scripts
        sudo groovy configure_portal_new.groovy > /var/lib/tomcat7/webapps/portal/config.js
    popd

    sudo /etc/init.d/tomcat7 start

popd
sudo rm -rf __tmpupdate__
