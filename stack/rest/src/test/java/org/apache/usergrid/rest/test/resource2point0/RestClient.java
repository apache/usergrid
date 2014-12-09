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

import java.io.IOException;
import java.net.URI;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.ITSetup;
import org.apache.usergrid.rest.RestITSuite;
import org.apache.usergrid.rest.test.resource2point0.endpoints.mgmt.ManagementResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.OrganizationResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource2point0.state.ClientContext;
import org.apache.usergrid.rest.test.security.TestAdminUser;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.test.framework.JerseyTest;


/**
 * Extends the JerseyTest framework because this is the client that we are going to be using to interact with tomcat
 */
public class RestClient implements UrlResource,TestRule {

//    @ClassRule
//    public static ITSetup setup = new ITSetup( RestITSuite.cassandraResource );

    private final String serverUrl;
    private final ClientContext context;
    WebResource resource;
    ClientConfig config = new DefaultClientConfig(  );//new ClientConfig();
    Client client = Client.create( config );

//This should work independantly of test frameowkr. Need to be able to pull the WebResource Url and not have to integrate the webresource into the Endpoints/Client.
    //This uses jeresy to create the client. Initialize the client with the webresource, and then the CLIENT calls the root resource.
    //
    //after initialization of the client htne use it to build our path using our resources.
    //Just keep checking in early and checkin often.

    public RestClient( final String serverUrl ) {
        this.serverUrl = serverUrl;
        this.context = new ClientContext();
        resource = client.resource( serverUrl );
        resource.path( serverUrl );

        //maybe the problem here is with the jaxrs version not having the correct dependencies or methods.
       // webTarget = client.target( serverUrl );
        //webTarget = webTarget.path( serverUrl );
    }


    /**
     * TODO: should this method return the base path or the total path we have built?
     * @return
     */
    @Override
    public String getPath() {
        return resource.getURI().toString(); //webResource.getUri().toString();
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
        resource = resource.path( organizationResource.getPath() );
        //webTarget = webTarget.path( organizationResource.getPath()); This worked really well, shame it couldn't be used.
        return new OrganizationResource( orgName, context,  this );
    }

//todo:fix this method for the client.
    public void loginAdminUser( final String username, final String password ) {
        //Post isn't implemented yet, but using the method below we should be able to get a superuser password as well.
        //final String token = management().token().post(username, password);

        //context.setToken( token );
    }

    //TODO: maybe take out the below methods to be a seperate class? Follow solid principles? Single responsiblitiy. This is currently
    //taking on the responsibility of both the

    @Override
    public Statement apply( Statement base, Description description ) {
        return statement( base, description );
    }

    private Statement statement( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );
                try {
                    base.evaluate();
                }
                finally {
                    cleanup();
                }
            }
        };
    }
    protected void cleanup() {
        // might want to do something here later
    }

    //TODO: look over this logic
    protected void before( Description description ) throws IOException {
//        String testClass = description.getTestClass().getName();
//        String methodName = description.getMethodName();
//        String name = testClass + "." + methodName;
//
//        TestAdminUser testAdmin = new TestAdminUser( name+ UUIDUtils.newTimeUUID(),
//                name + "@usergrid.com"+UUIDUtils.newTimeUUID(),
//                name + "@usergrid.com"+UUIDUtils.newTimeUUID() );
//        withOrg( name+ UUIDUtils.newTimeUUID() ).withApp( methodName + UUIDUtils.newTimeUUID() ).withUser(
//                testAdmin ).initAll();
    }
}
