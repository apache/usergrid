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
// configure_usergrid.groovy 
// 
// Register this host machine as a Cassandra node in our stack. 
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
String domain    = stackName
String nodeType = + args[0];

if (args.size() != 1 )  {
  println "this script expects one argument.  registry_register.groovy nodeType"
  // You can even print the usage here.
  return
}


def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

// creates domain or no-op if it already exists
sdbClient.createDomain(new CreateDomainRequest(domain))

def gar = new GetAttributesRequest(domain, hostName);
def response = sdbClient.getAttributes(gar);
if (response.getAttributes().size() == 1) {
    println "Already registered"
    def attrs = response.getAttributes()
    for (att in attrs) {
        println("${hostName} -> ${att.getName()} : ${att.getValue()}")
    }
} else {
    println "Registering..."
    def stackAtt = new ReplaceableAttribute("nodetype", nodeType, true)
    def attrs = new ArrayList()
    attrs.add(stackAtt)
    def par = new PutAttributesRequest(domain, hostName, attrs)
    sdbClient.putAttributes(par);
    println "Registraition done."
}
