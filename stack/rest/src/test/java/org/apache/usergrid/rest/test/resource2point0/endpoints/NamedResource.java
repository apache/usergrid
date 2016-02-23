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


import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Base class that is extended by named endpoints.
 * The NamedResource stores the parent of the class, the context in which the class operates and then Name of this resource
 */
public abstract class NamedResource implements UrlResource {

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
        return getResource(useToken,null);
    }
    public WebResource getResource(boolean useToken,Token token) {
        WebResource resource = parent.getResource().path( getPath() );
        token = token !=null ? token : this.context.getToken();
        return  useToken    ? resource.queryParam("access_token",token.getAccessToken()) :  parent.getResource().path( getPath() );
    }

    protected WebResource addParametersToResource(WebResource resource, final QueryParameters parameters){

        if(parameters == null){
            return resource;
        }
        if ( parameters.getQuery() != null ) {
            resource = resource.queryParam( "ql", parameters.getQuery() );
        }

        if ( parameters.getCursor() != null ) {
           resource = resource.queryParam( "cursor", parameters.getCursor() );
        }

        if ( parameters.getStart() != null ) {
            resource = resource.queryParam("start", parameters.getStart().toString());
        }

        if ( parameters.getLimit() != null ) {
             resource = resource.queryParam("limit", parameters.getLimit().toString());
        }
        //We can also post the params as queries
        if ( parameters.getFormPostData().size() > 0){
            Map<String,String> formData = parameters.getFormPostData();
            Set<String> keySet = formData.keySet();
            Iterator<String> keyIterator = keySet.iterator();


            while(keyIterator.hasNext()){
                String key = keyIterator.next();
                String value = formData.get( key );
                resource = resource.queryParam( key, value );
            }
        }
        return resource;
    }

    public <T> T post(Class<T> type, Entity requestEntity) {
        return post(true,type,requestEntity,null,false);

    }

    //Used for empty posts
    public <T> T post( boolean useToken, Class<T> type, Map entity, final QueryParameters queryParameters, boolean useBasicAuthentication ) {
        WebResource resource = getResource(useToken);
        resource = addParametersToResource(resource, queryParameters);
        WebResource.Builder builder = resource
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept( MediaType.APPLICATION_JSON );

        if(entity!=null){
            builder.entity(entity);
        }

        if(useBasicAuthentication){
            //added httpBasicauth filter to all setup calls because they all do verification this way.
            HTTPBasicAuthFilter httpBasicAuthFilter = new HTTPBasicAuthFilter( "superuser","superpassword" );
            resource.addFilter(httpBasicAuthFilter);
        }

        GenericType<T> gt = new GenericType<>((Class) type);
        return builder.post(gt.getRawClass());

    }


}
