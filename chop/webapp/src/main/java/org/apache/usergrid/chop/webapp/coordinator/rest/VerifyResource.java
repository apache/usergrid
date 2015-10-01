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


import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.Result;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.stack.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


/**
 * REST operation to setup the Stack under test.
 */
@Singleton
@Produces(MediaType.APPLICATION_JSON)
@Path(VerifyResource.ENDPOINT_URL)
public class VerifyResource {
    public final static String ENDPOINT_URL = "/verify";
    private static final Logger LOG = LoggerFactory.getLogger(VerifyResource.class);


    @Inject
    public VerifyResource() {
    }


    @POST
    public Result setup(Stack stack) {
        LOG.warn("Calling setup");
        return new BaseResult(ENDPOINT_URL, true, "Setup called", State.READY);
    }
}
