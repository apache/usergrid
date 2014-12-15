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
package org.apache.usergrid.rest.test.resource2point0.endpoints;


import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.sun.jersey.api.client.WebResource;

import java.util.UUID;


/**
 * Base class that is extended by named endpoints.
 * The NamedResource stores the parent of the class, the context in which the class operates and then Name of this resource
 */
public class NamedResource implements UrlResource {

    protected final String name;
    protected final ClientContext context;
    /* Stores the path of the parent that called it.
    i.e If we had a ApplicationResource ( an instance of a namedResource ) this would contain the OrganizationResource.
     */
    protected final UrlResource parent;


    public NamedResource( final String name, final ClientContext context, final UrlResource parent ) {
        this.name = name;
        this.context = context;
        this.parent = parent;
    }


    @Override
    public String getPath() {
        return name;
    }

    @Override
    public WebResource getResource() {
        return getResource(false);
    }

    public WebResource getResource(boolean useToken) {
        WebResource resource = parent.getResource().path( getPath() );
        return useToken ? resource.queryParam("access_token",this.context.getToken().getAccessToken()) :  parent.getResource().path( getPath() );
    }

    protected WebResource addParametersToResource(final WebResource resource, final QueryParameters parameters){
        if(parameters == null){
            return resource;
        }
        if ( parameters.getQuery() != null ) {
            resource.queryParam( "ql", parameters.getQuery() );
        }

        if ( parameters.getCursor() != null ) {
           resource.queryParam( "cursor", parameters.getCursor() );
        }

        if ( parameters.getStart() != null ) {
            resource.queryParam("start", parameters.getStart().toString());
        }

        if ( parameters.getLimit() != null ) {
             resource.queryParam("limit", parameters.getLimit().toString());
        }
        return resource;
    }


}
