import groovy.json.JsonOutput

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
def createMetric(def title, def collectdMetric, def servers, def array) {

    def serversJson = []

    for (server in servers) {

        def normalizedServer = server.replaceAll("\\.", "_")

        serversJson.add("collectd.${normalizedServer}.${collectdMetric}")

    }


    def metric = ["target": serversJson, "title": title]

    array.add(metric)

}


NodeRegistry registry = new NodeRegistry();


def servers = registry.searchNode("rest")



def json = []

createMetric("Used Memory", "memory.memory-used", servers, json)

createMetric("Free Memory", "memory.memory-free", servers, json)

createMetric("Load Short Term", "load.load.shortterm", servers, json)

createMetric("Network Received", "interface-eth0.if_octets.rx", servers, json)

createMetric("Network Sent", "interface-eth0.if_packets.tx", servers, json)

createMetric("Tomcat Heap", "GenericJMX-memory-heap.memory-used", servers, json)

createMetric("Tomcat Non Heap", "GenericJMX-memory-nonheap.memory-used", servers, json)

createMetric("Tomcat Old Gen", "GenericJMX-memory_pool-CMS_Old_Gen.memory-used", servers, json)

createMetric("Tomcat Permgen", "GenericJMX-memory_pool-CMS_Perm_Gen.memory-used", servers, json)



def jsonString = JsonOutput.toJson(json)
println JsonOutput.prettyPrint(jsonString)


