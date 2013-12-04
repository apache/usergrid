/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.perftest.rest;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.perftest.amazon.AmazonS3Service;
import org.apache.usergrid.perftest.amazon.Ec2Metadata;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;
import java.util.Set;


/**
 * ...
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( "/runners" )
public class RunnersResource {
    private final AmazonS3Service service;


    @Inject
    public RunnersResource( AmazonS3Service service ) {
        this.service = service;
    }


    @GET
    public Map<String,Ec2Metadata> getRunners() throws IOException {
        return service.getRunners();
    }
}
