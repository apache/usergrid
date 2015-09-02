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

package org.apache.usergrid.rest.test.resource.endpoints;


import javax.ws.rs.core.MediaType;

import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;


/**
 * Functions as the endpoint for all resources that hit /system/ * /setup
 */
public class BoostrapResource extends NamedResource {

    public BoostrapResource( final ClientContext context, final UrlResource parent ) {
        super("bootstrap",context,parent);
    }

    public Entity put(QueryParameters queryParameters){

        WebResource resource = getResource();
        resource = addParametersToResource( resource, queryParameters );

        //added httpBasicauth filter to all setup calls because they all do verification this way.
        HTTPBasicAuthFilter httpBasicAuthFilter = new HTTPBasicAuthFilter( "superuser","superpassword" );
        resource.addFilter( httpBasicAuthFilter );

        return resource.type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                                .put( Entity.class );
    }
}
