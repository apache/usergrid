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


import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Root resource for stuff
 */
public class RootResource implements UrlResource {


    private final String serverUrl;
    private final ClientContext context;


    public RootResource( final String serverUrl, final ClientContext context ) {this.serverUrl = serverUrl;
        this.context = context;
    }


    @Override
    public String getPath() {
        return serverUrl;
    }


    /**
     * Get the management resource
     * @return
     */
    public ManagementResource management(){
        return new ManagementResource( context, this);
    }


    /**
     * Get hte organization resource
     * @param orgName
     * @return
     */
    public OrganizationResource  org(final String orgName){
        return new OrganizationResource( orgName,context,  this );
    }
}
