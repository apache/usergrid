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
import org.apache.usergrid.perftest.PerftestRunner;
import org.apache.usergrid.perftest.amazon.AmazonS3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


/**
 * Loads a test configuration from the "tests" container.
 */
@Singleton
@Produces( MediaType.APPLICATION_JSON )
@Path( "/load" )
public class LoadResource extends PropagatingResource {
    private static final Logger LOG = LoggerFactory.getLogger( LoadResource.class );
    private final PerftestRunner runner;


    @Inject
    public LoadResource( PerftestRunner runner, AmazonS3Service service ) {
        super( "/load", service );
        this.runner = runner;
    }


    /**
     * By default the propagate parameter is considered to be false unless set
     * to true. To propagate this call to all the other runners this parameter
     * will be set to true.
     *
     * @param propagate when true call the same function on other runners
     * @param perftest the perftest to use specified by the string containing the
     *                 <git-uuid>-<deploy-timestamp>
     * @return a summary message
     */
    @POST
    public Result load( @QueryParam( "propagate" ) Boolean propagate,
                        @QueryParam( "perftest" ) String perftest ) {
        LOG.debug( "The propagate request parameter was set to {}", propagate );

        if ( runner.isRunning() ) {
            return new BaseResult( getEndpointUrl(), false, "still running stop and reset before loading a new test" );
        }

        if ( runner.needsReset() ) {
            return new BaseResult( getEndpointUrl(), false, "reset before loading a new test" );
        }

        // now we need to actually handle loading here

        if ( propagate == Boolean.FALSE )
        {
            // need to just load here
        }

        return propagate( true, "" );
    }
}
