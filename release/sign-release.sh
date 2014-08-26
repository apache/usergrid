#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

pushd target 

export rcstring="-rc2"
export vstring="1.0.0"

gpg --armor --detach-sig apache-usergrid-incubating-${vstring}${rcstring}-source.zip
gpg --armor --detach-sig apache-usergrid-incubating-${vstring}${rcstring}-source.tar.gz

gpg --print-md MD5 apache-usergrid-incubating-${vstring}${rcstring}-source.zip > apache-usergrid-incubating-${vstring}${rcstring}-source.zip.md5
gpg --print-md MD5 apache-usergrid-incubating-${vstring}${rcstring}-source.tar.gz > apache-usergrid-incubating-${vstring}${rcstring}-source.tar.gz.md5

popd
