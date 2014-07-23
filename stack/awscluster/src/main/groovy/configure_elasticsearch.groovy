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
// configure_cassandra.groovy 
// 
// Emits Cassandra config file based on environment and Cassandra node 
// registry in SimpleDB
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
String domain    = stackName

def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

// build seed list by listing all Cassandra nodes found in SimpleDB domain with our stackName
def selectResult = sdbClient.select(new SelectRequest((String)"select * from `${domain}` where itemName() is not null and nodeType = 'elasticsearch'  order by itemName()"))
def seeds = ""
def sep = ""
for (item in selectResult.getItems()) {
    def att = item.getAttributes().get(0)
    if (att.getValue().equals(stackName)) {
        seeds = "${seeds}${sep}\"${item.getName()}\""
        sep = ","
    }
}

def elasticSearchConfig = """
cluster.name: usergrid2
discovery.zen.ping.multicast.enabled: false
discovery.zen.ping.unicast.hosts: [${seeds}]
node:
    name: ${hostName} 
network:
    host: ${hostName}
path:
    logs: /mnt/log/elasticsearch
    data: /mnt/data/elasticsearch
"""

println elasticSearchConfig
