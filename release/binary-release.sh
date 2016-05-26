#!/bin/bash

#---------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one or more 
# contributor license agreements.  See the NOTICE file distributed with 
# this work for additional information regarding copyright ownership.  
# The ASF licenses this file to you under the Apache License, Version 2.0 
# (the "License"); you may not use this file except in compliance with the 
# License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software 
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See 
# the License for the specific language governing permissions and 
# limitations under the License.
#---------------------------------------------------------------------------

if [[ $# < "1" ]]; then
    echo "Must specify version on command line"
    exit -1;
fi

echo "Building binary distribution using version" $1

# Attempt to build Java SDK, Portal, Stack and Tools

pushd ../sdks/java
mvn -DskipTests=true clean install
popd

pushd ../portal
./build.sh 
popd

pushd ../stack
mvn -DskipTests=true clean install
cd tools
mvn -DskipTests=true clean install
popd

# assemble binary release
mvn -DreleaseVersion=$1 clean install

