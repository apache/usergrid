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

TILE_METADATA_VERSION=1.6

# Make sure docker-boshrelease-23.tgz tar ball is available

mkdir -p product/metadata product/releases product/content_migrations # product/javascript-migrations
cp dev_releases/apache*/apache*tgz product/releases
cp docker-boshrelease-23.tgz product/releases
cp apache-usergrid-tile-${TILE_METADATA_VERSION}.yml product/metadata/apache-usergrid.yml
cp content_migrations.yml product/content_migrations
cd product
zip -r ../usergrid.pivotal *
cd ..
