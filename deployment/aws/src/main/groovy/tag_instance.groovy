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
// tag_instance.groovy 
// 
// Tag instance so we can easily identify it in the EC2 console 
//
import com.amazonaws.auth.*
import com.amazonaws.services.ec2.*
import com.amazonaws.services.ec2.model.*

String type       = (String)System.getenv().get("TYPE")
String accessKey  = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey  = (String)System.getenv().get("AWS_SECRET_KEY")
String instanceId = (String)System.getenv().get("EC2_INSTANCE_ID")
String stackName  = (String)System.getenv().get("STACK_NAME")


String moreMetaData = ""

if (args.size() == 1 )  {
    moreMetaData = args[0]
}


def creds = new BasicAWSCredentials(accessKey, secretKey)
def ec2Client = new AmazonEC2Client(creds)

def resources = new ArrayList()
resources.add(instanceId)

def tags = new ArrayList()
def tag = "${stackName}-${type}-${instanceId}${moreMetaData}"
tags.add(new Tag("Name", tag))

ec2Client.createTags(new CreateTagsRequest(resources, tags))

println "Tagged instance as ${tag}"
