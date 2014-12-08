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
package org.apache.usergrid.rest.test.resource2point0;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.JerseyWebTarget;
import org.junit.ClassRule;

import org.apache.usergrid.rest.ITSetup;
import org.apache.usergrid.rest.RestITSuite;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.OrganizationResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;


/**
 * Extends the JerseyTest framework because this is the client that we are going to be using to interact with tomcat
 */
public class RestClient implements UrlResource {

    ClientConfig clientConfig = new ClientConfig();

//    @ClassRule
//    public static ITSetup setup = new ITSetup( RestITSuite.cassandraResource );

    Client client = ClientBuilder.newClient( clientConfig );

    private WebTarget webTarget;// = client.target("http://example.com/rest");

    private final String serverUrl;
    private final ClientContext context;
    //ClientConfig config = new ClientConfig();


//This should work independantly of test frameowkr. Need to be able to pull the WebResource Url and not have to integrate the webresource into the Endpoints/Client.
    //This uses jeresy to create the client. Initialize the client with the webresource, and then the CLIENT calls the root resource.
    //
    //after initialization of the client htne use it to build our path using our resources.
    //Just keep checking in early and checkin often.

    public RestClient( final String serverUrl ) {
        this.serverUrl = serverUrl;
        this.context = new ClientContext();
        webTarget = client.target( serverUrl );
        //webTarget = webTarget.path( serverUrl );
    }


    /**
     * TODO: should this method return the base path or the total path we have built?
     * @return
     */
    @Override
    public String getPath() {
        return webTarget.getUri().toString();
        //return serverUrl;
    }


    /**
     * Get the management resource
     */
    public ManagementResource management() {
        return new ManagementResource( context, this );
    }


    /**
     * Get hte organization resource
     */
    public OrganizationResource org( final String orgName ) {
        OrganizationResource organizationResource = new OrganizationResource( orgName, context,  this );
        webTarget = webTarget.path( organizationResource.getPath());
        return new OrganizationResource( orgName, context,  this );
    }

//todo:fix this method for the client.
    public void loginAdminUser( final String username, final String password ) {
        //Post isn't implemented yet, but using the method below we should be able to get a superuser password as well.
        //final String token = management().token().post(username, password);

        //context.setToken( token );
    }
}
