#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


# Injected Dynamic ENV Parameters
#################################

# The cluster name in caps is used as prefix for the dynamic parameters
# like _HOSTS and _ADDRS. The order (index) of a specific hostname in             
# the space separate list, will match the position in the space separated
# list with its associated IPv4 public address


# ELASTICSEARCH_HOSTS = dynamic space sep list of cas hostnames (public)
# ELASTICSEARCH_ADDRS = dynamic space sep list of cas host ip addresses (public)
# CLUSTER_NAME = statically set ENV parameter in the stack.json file

# Setup Sequence Below
######################


