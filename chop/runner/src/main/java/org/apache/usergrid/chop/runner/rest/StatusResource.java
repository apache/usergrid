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
package org.apache.usergrid.chop.runner.rest;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.Runner;
import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.Result;
import org.apache.usergrid.chop.runner.IController;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** ... */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( Runner.STATUS_GET )
public class StatusResource {
    private final IController controller;
    private final Project project;


    @Inject
    public StatusResource( IController controller, Project project ) {
        this.controller = controller;
        this.project = project;
    }


    @GET
    public Result status() {
        return new BaseResult( Runner.STATUS_GET, true, "status request", controller.getState(), project );
    }
}
