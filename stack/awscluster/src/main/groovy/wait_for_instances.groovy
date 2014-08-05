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


if (args.size() !=2 )  {
  println "this script expects two arguments.  wait_for_instances.groovy nodetype numberOfServers"
  // You can even print the usage here.
  return 1;
}

String nodetype = args[0]
int numberOfServers = args[1].toInteger()


NodeRegistry registry = new NodeRegistry();

println "Waiting for ${numberOfServers} nodes of type ${nodetype} to register..."

def count = 0

while (true) {
    try {
        def selectResult = registry.searchNode(nodetype)

        count = selectResult.size();

        if (count >= numberOfServers) {
            println("count = ${count}, total number of servers is ${numberOfServers}.  Breaking")
            break
        }

        println("Found ${count} nodes but need at least ${numberOfServers}.  Waiting...")
    } catch (Exception e) {
        println "ERROR waiting for Casasndra ${e.getMessage()}, will continue waiting"
    }
    Thread.sleep(2000)
}

println "Waiting done."
