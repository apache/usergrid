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


import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;

import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Base class that is extended by named endpoints.
 * The NamedResource stores the parent of the class, the context in which the class operates and then Name of this resource
 */
public abstract class NamedResource<T extends NamedResource<T>> implements UrlResource {

    protected String name;
    protected ClientContext context;
    /* Stores the path of the parent that called it.
    i.e If we had a ApplicationResource ( an instance of a namedResource ) this would contain the OrganizationResource.
     */
    protected UrlResource parent;

    protected abstract T getThis();

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

    public T withToken(Token token){
        T generic=this.getThis();
        generic.context.setToken(token);
        return generic;
    }
    public <Type> Type post() {
        return ((Type) this.post(new Entity(), this.context.getToken()));
    }
    public <Type> Type post(Object requestEntity) {
        return ((Type) this.post(requestEntity,this.context.getToken()));
    }
    public <Type> Type post(Object requestEntity, Token token) {
        WebResource resource = getResource(true, token);
        return ((Type) resource.type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON).post(Entity.class, requestEntity));
    }
    public <Type> Type post(List<Type> requestEntity, Token token) {
        WebResource resource = getResource(true, token);
        return ((Type) resource.type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON).post(Entity.class, requestEntity));
    }
    public <Type> Type put() {
        return ((Type) this.put(new Entity(), this.context.getToken()));
    }
    public <Type> Type put(Object requestEntity) {
        return ((Type) this.put(requestEntity, this.context.getToken()));
    }
    public <Type> Type put(Object requestEntity, Token token) {
        WebResource resource = getResource(true, token);
        return ((Type) resource.type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON).put(Entity.class, requestEntity));
    }
    public <Type> Type get() {
        return ((Type) this.getResource(true, this.context.getToken()).get(Entity.class));
    }

}
