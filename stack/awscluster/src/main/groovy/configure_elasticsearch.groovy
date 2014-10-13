/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */



// 
// configure_elasticsearch.groovy 
// 
// Emits Elasticsearch config file based on environment and Elasticsearch node 
// registry in SimpleDB
//

import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
def clusterName  = (String)System.getenv().get("ES_CLUSTER_NAME")

NodeRegistry registry = new NodeRegistry();

// build seed list by listing all Elasticsearch nodes found in SimpleDB domain with our stackName
def selectResult = registry.searchNode('elasticsearch')
def esnodes = ""
def sep = ""
for (hostname in selectResult) {
   esnodes = "${esnodes}${sep}\"${hostname}\""
   sep = ","
}

def elasticSearchConfig = """
cluster.name: ${clusterName}
discovery.zen.minimum_master_nodes: 4
discovery.zen.ping.multicast.enabled: false
discovery.zen.ping.unicast.hosts: [${esnodes}]
node:
    name: ${hostName} 
network:
    host: ${hostName}
path:
    logs: /mnt/log/elasticsearch
    data: /mnt/data/elasticsearch
bootstrap.mlockall: true
threadpool.index.type: fixed
threadpool.index.size: 160
threadpool.index.queue_size: 401
threadpool.bulk.type: fixed
threadpool.bulk.size: 160
threadpool.bulk.queue_size: 800

action.auto_create_index: false

action.disable_delete_all_indices: true
"""

println elasticSearchConfig
