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
package org.apache.usergrid.chop.webapp.coordinator.rest;


import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * REST operation to setup the Stack under test.
 */
@Singleton
@Produces(MediaType.TEXT_PLAIN)
@Path(TestGetResource.ENDPOINT_URL)
public class TestGetResource {
    public final static String TEST_MESSAGE = "/testget called";
    public final static String ENDPOINT_URL = "/testget";
    private static final Logger LOG = LoggerFactory.getLogger(TestGetResource.class);


    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    public String testget() {
        LOG.warn("Calling testget");
        return TEST_MESSAGE;
    }
}
