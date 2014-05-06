/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.runner.rest;


import javax.ws.rs.core.Response;

import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.Signal;
import org.apache.usergrid.chop.api.State;
import org.apache.usergrid.chop.runner.IController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A base class for all signal resources.
 */
public abstract class SignalResource extends TestableResource {
    private static final Logger LOG = LoggerFactory.getLogger( SignalResource.class );

    private final Project project;
    private final IController controller;
    private final String endpoint;
    private final Signal signal;


    protected SignalResource( IController controller, Project project, String endpoint, Signal signal ) {
        super( endpoint );
        this.endpoint = endpoint;
        this.project = project;
        this.controller = controller;
        this.signal = signal;
    }


    public Response op( boolean inTestMode ) {
        State state = controller.getState();
        BaseResult result = new BaseResult();
        result.setState( state );
        result.setMessage( state.getMessage( signal ) );
        result.setProject( project );
        result.setEndpoint( endpoint );

        if ( inTestMode ) {
            result.setStatus( true );
            result.setMessage( getTestMessage() );
            LOG.info( getTestMessage() );
            return Response.ok( result ).build();
        }

        if ( state.accepts( signal ) ) {
            controller.send( signal );
            result.setState( controller.getState() );
            result.setStatus( true );
            LOG.info( result.getMessage() );
            return Response.ok( result ).build();
        }

        result.setStatus( false );
        LOG.warn( result.getMessage() ); // ==> got message from state
        return Response.status( Response.Status.CONFLICT ).entity( result ).build();
    }
}
