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

/**
 * A utility class that search simple db for the node type provided and returns a list of hostnames as a string array
 */
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpledb.AmazonSimpleDBClient
import com.amazonaws.services.simpledb.model.*

class NodeRegistry {

    private String accessKey = (String) System.getenv().get("AWS_ACCESS_KEY")
    private String secretKey = (String) System.getenv().get("AWS_SECRET_KEY")
    private String stackName = (String) System.getenv().get("STACK_NAME")
    private hostName = (String) System.getenv().get("PUBLIC_HOSTNAME")
    private String domain = stackName

    private def creds;
    private def sdbClient;


    NodeRegistry() {
        while ( true ) {
            try {
                // creates domain or no-op if it already exists
                creds = new BasicAWSCredentials(accessKey, secretKey)
                sdbClient = new AmazonSimpleDBClient(creds)
                sdbClient.createDomain(new CreateDomainRequest(domain))

            } catch ( Exception e ) {
                continue
            }
            break
        }
    }

    /**
     * Search for the node type, return a string array of hostnames that match it within the running domain
     * @param defNodeType
     */
    def searchNode(def nodeType) {
        def selectResult = sdbClient.select(new SelectRequest((String) \
            "select * from `${domain}` where itemName() is not null and nodetype = '${nodeType}'  order by itemName()"))
        def result = []

        for (item in selectResult.getItems()) {
            def hostname = item.getName()
            result.add(hostname)
        }

        return result

    }

    /**
     * Get the entire database back in raw form
     * @return
     */
    def selectAll() {
        def selectResult = sdbClient.select(new SelectRequest((String) "select * from `${domain}`")).getItems()

        return selectResult;
    }

    /**
     * Add the node to the database if it doesn't exist
     */
    def addNode(def nodeType) {
        def gar = new GetAttributesRequest(domain, hostName);
        def response = sdbClient.getAttributes(gar);
        if (response.getAttributes().size() == 1) {
            println "Already registered"
            def attrs = response.getAttributes()
            for (att in attrs) {
                println("${hostName} -> ${att.getName()} : ${att.getValue()}")
            }

            return false;

        } else {
            println "Registering..."
            def stackAtt = new ReplaceableAttribute("nodetype", nodeType, true)
            def attrs = new ArrayList()
            attrs.add(stackAtt)
            def par = new PutAttributesRequest(domain, hostName, attrs)
            sdbClient.putAttributes(par);
            println "Registraition done."
            return true;
        }


    }

    def deleteRegistry(){
        sdbClient.deleteDomain(new DeleteDomainRequest(domain))
    }

}
