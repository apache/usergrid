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
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.*

class NodeRegistry {


    public static final String TAG_PREFIX = "tag:"
    //taken from aws
    public static final String STACK_NAME = "usergrid:stack-name";
    public static final String NODE_TYPE = "usergrid:node_type";
    public static final String SEARCH_INSTANCE_STATE = "instance-state-name";
    public static final String SEARCH_STACK_NAME = TAG_PREFIX + STACK_NAME
    public static final String SEARCH_NODE_TYPE = TAG_PREFIX + NODE_TYPE

    private String accessKey = (String) System.getenv().get("AWS_ACCESS_KEY")
    private String secretKey = (String) System.getenv().get("AWS_SECRET_KEY")
    private String stackName = (String) System.getenv().get("STACK_NAME")
    private String instanceId = (String) System.getenv().get("EC2_INSTANCE_ID");
    private String region = (String) System.getenv().get("EC2_REGION");
    private String domain = stackName

    private BasicAWSCredentials creds;
    private AmazonEC2Client ec2Client;


    NodeRegistry() {

        if (region == null) {
            throw new IllegalArgumentException("EC2_REGION must be defined")
        }

        if (instanceId == null) {
            throw new IllegalArgumentException("EC2_INSTANCE_ID must be defined")
        }

        if (stackName == null) {
            throw new IllegalArgumentException("STACK_NAME must be defined")
        }

        if (accessKey == null) {
            throw new IllegalArgumentException("AWS_ACCESS_KEY must be defined")
        }

        if (secretKey == null) {
            throw new IllegalArgumentException("AWS_SECRET_KEY must be defined")
        }

        creds = new BasicAWSCredentials(accessKey, secretKey)
        ec2Client = new AmazonEC2Client(creds)
        def regionEnum = Regions.fromName(region);
        ec2Client.setRegion(Region.getRegion(regionEnum))


    }

    /**
     * Search for the node type, return a string array of hostnames that match it within the running domain
     * @param defNodeType
     */
    def searchNode(def nodeType) {


        def stackNameFilter = new Filter(SEARCH_STACK_NAME).withValues(stackName)
        def nodeTypeFilter = new Filter(SEARCH_NODE_TYPE).withValues(nodeType)
        def instanceState = new Filter(SEARCH_INSTANCE_STATE).withValues(InstanceStateName.Running.toString());

        //sort by created date
        def servers = new TreeSet<ServerEntry>();


        def token = null



        while (true) {

            def describeRequest = new DescribeInstancesRequest().withFilters(stackNameFilter, nodeTypeFilter, instanceState)

            if (token != null) {
                describeRequest.withNextToken(token);
            }


            def nodes = ec2Client.describeInstances(describeRequest)

            for (reservation in nodes.getReservations()) {

                for (instance in reservation.getInstances()) {
                    servers.add(new ServerEntry(instance.launchTime, instance.publicDnsName));
                }

            }

            //nothing to do, exit the loop
            if (nodes.nextToken == null) {
                break;
            }

            token = nodes.nextToken;

        }




        return createResults(servers);
    }

    def createResults(def servers) {

        def results = [];

        for (server in servers) {
            results.add(server.publicIp)
        }

        return results;
    }

    /**
     * Add the node to the database if it doesn't exist
     */
    def addNode(def nodeType) {

        //add the node type
        def tagRequest = new CreateTagsRequest().withTags(new Tag(NODE_TYPE, nodeType), new Tag(STACK_NAME, stackName)).withResources(instanceId)



        ec2Client.createTags(tagRequest)


    }

    /**
     * Wait until the number of servers are available with the type specified
     * @param nodeType
     * @param count
     */
    def waitUntilAvailable(def nodeType, def numberOfServers){

        while (true) {
            try {
                def selectResult = searchNode(nodeType)

                def count = selectResult.size();

                if (count >= numberOfServers) {
                    println("count = ${count}, total number of servers is ${numberOfServers}.  Breaking")
                    break
                }

                println("Found ${count} nodes but need at least ${numberOfServers}.  Waiting...")
            } catch (Exception e) {
                println "ERROR waiting for ${nodeType} ${e.getMessage()}, will continue waiting"
            }
            Thread.sleep(2000)
        }
    }


    class ServerEntry implements Comparable<ServerEntry> {
        private final Date launchDate;
        private final String publicIp;

        ServerEntry(final Date launchDate, final String publicIp) {
            this.launchDate = launchDate
            this.publicIp = publicIp
        }

        @Override
        int compareTo(final ServerEntry o) {

            int compare = launchDate.compareTo(o.launchDate)

            if(compare == 0){
                compare =  publicIp.compareTo(o.publicIp);
            }

            return compare
        }

        boolean equals(final o) {
            if (this.is(o)) return true
            if (getClass() != o.class) return false

            final ServerEntry that = (ServerEntry) o

            if (launchDate != that.launchDate) return false
            if (publicIp != that.publicIp) return false

            return true
        }

        int hashCode() {
            int result
            result = launchDate.hashCode()
            result = 31 * result + publicIp.hashCode()
            return result
        }
    }

}
