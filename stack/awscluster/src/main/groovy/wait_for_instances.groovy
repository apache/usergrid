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
// wait_for_instances.groovy 
// 
// Wait for enough Cassandra servers are up before proceding,
// Enough means count greater than or equal to replication factor. 
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
String domain    = stackName

//def replicationFactor = System.getenv().get("CASSANDRA_REPLICATION_FACTOR")
int cassNumServers = System.getenv().get("CASSANDRA_NUM_SERVERS").toInteger()

def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

println "Waiting for Cassandra nodes to register..."
    
def count = 0

while (true) {
    try {
        def selectResult = sdbClient.select( new SelectRequest(
            (String)"select * from `${domain}` where itemName() is not null  order by itemName()"))

        count = 0

        for (item in selectResult.getItems()) {
            def att = item.getAttributes().get(0)
            if (att.getValue().equals(stackName)) {
                count++
            }
        }
        if (count >= cassNumServers) {
            println("count = ${count}, total number of servers is ${cassNumServers}.  Breaking")
            break
        }

        println("Found ${count} nodes but need at least ${cassNumServers}.  Waiting...")
        
    } catch (Exception e) {
        println "ERROR waiting for Casasndra ${e.getMessage()}, will continue waiting"
        return
    }
    Thread.sleep(2000)
}

println "Waiting done."
