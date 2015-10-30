/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest.filters;


import org.apache.usergrid.persistence.cassandra.util.TraceTag;
import org.apache.usergrid.persistence.cassandra.util.TraceTagManager;
import org.apache.usergrid.persistence.cassandra.util.TraceTagReporter;
import org.apache.usergrid.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.*;
import javax.ws.rs.core.Context;


/**
 * Attach and detach trace tags at start and end of request scopes
 *
 * @author zznate
 */
@Resource
@PreMatching
@Component
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private Logger logger = LoggerFactory.getLogger( TracingFilter.class );

    @Autowired
    private TraceTagManager traceTagManager;
    @Autowired
    private TraceTagReporter traceTagReporter;


    @Context
    private HttpServletRequest httpServletRequest;


    @Override
    public void filter( ContainerRequestContext request ) {
        if ( !traceTagManager.getTraceEnabled() && !traceTagManager.getExplicitOnly() ) {
            return;
        }
        String traceId;
        if ( traceTagManager.getExplicitOnly() ) {
            // if we are set in explicit mode and the header is not present, leave.
            String id = httpServletRequest.getHeader( "XX-TRACE-ID" );
            if ( StringUtils.isBlank( id ) ) {
                return;
            }
            traceId = id.concat( "-REST-" ).concat( request.getUriInfo().getPath( true ) );
        }
        else {
            traceId = "TRACE-".concat( request.getUriInfo().getPath( true ) );
        }
        TraceTag traceTag = traceTagManager.create( traceId );
        traceTagManager.attach( traceTag );
    }


    @Override
    public void filter( ContainerRequestContext request, ContainerResponseContext response ) {
        if ( traceTagManager.isActive() ) {
            TraceTag traceTag = traceTagManager.detach();
            traceTagReporter.report( traceTag );
        }
    }

}
