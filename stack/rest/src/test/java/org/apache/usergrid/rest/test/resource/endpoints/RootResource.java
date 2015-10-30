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


import org.apache.usergrid.rest.test.resource.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import javax.ws.rs.client.WebTarget;


/**
 * Contains the root element for classes. Contains the client context that holds a token for the management calls,
 * and also contains the serverUrl so we know what endpoint we need to hit.
 * Contains the two top level functions that can be called from the "root" ( actual root is the serverUrl )
 * 1.) Is the management resource i.e /management/org/blah/...
 * 2.) Is the organization resource i.e /<orgname>/<appname>...
 * This is where top level elements are contained and managemend
 */
//TODO: check to see if this actually ever gets called. It doesn't seem like so remove once verified.
public class RootResource implements UrlResource {


    private final String serverUrl;
    private final ClientContext context;


    /**
     *
     * @param serverUrl The serverurl that has stood up the UG instance i.e localhost:8080
     * @param context Contains the token that will be used for the following resources.
     */
    public RootResource( final String serverUrl, final ClientContext context ) {
        this.serverUrl = serverUrl;
        this.context = context;
    }


    /**
     * Returns the serverUrl that the root resource is pointing to.
     * @return serverUrl
     */
    @Override
    public String getPath() {
        return serverUrl;
    }

    @Override
    public WebTarget getTarget() {
        //TODO: fix this to return the proper resource in the scope we expect it, might not be needed here.
        return null;
    }

    @Override
    public ClientContext getContext() {
        return context;
    }

    /**
     * Get the management resource
     * @return
     */
    public ManagementResource management(){
        return new ManagementResource( context, this);
    }


    /**
     * Get the organization resource
     * @param orgName
     * @return OrganizationResource Returns an instance of the OrganizationResource to continue builder pattern
     */
    public OrganizationResource  org(final String orgName){
        return new OrganizationResource( orgName,context,  this );
    }
}
