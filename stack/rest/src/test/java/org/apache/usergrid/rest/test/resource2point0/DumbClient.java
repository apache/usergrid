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

import org.glassfish.jersey.client.ClientConfig;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.catalina.startup.Tomcat;

import org.apache.usergrid.TomcatMain;
import org.apache.usergrid.rest.TomcatResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.ApplicationResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.Collection;
import org.apache.usergrid.rest.test.resource2point0.endpoints.OrganizationResource;
import org.apache.usergrid.rest.test.resource2point0.endpoints.RootResource;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.EntityResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Test Class used to model if the client is working or doing what it is supposed to be doing.
 * ask if this is for test purposes and if so if I can mark it with junit
 */

public class DumbClient extends AbstractRestIT {

    //TODO: maybe this should just take in the raw uri.
    private final RestClient client = new RestClient( getBaseUri().toString());


    @Test
    public void stuff(){
        //EntityResponse itr  =  client.org( "test" ).getApp( "test" ).users().getEntityResponse();
        OrganizationResource organizationResource = client.org( "borg" );
        assertNotNull( organizationResource );https://community.spotify.com/t5/forums/replypage/board-id/spotifyiOS/message-id/42230
        assertEquals( getBaseUri().toString()+"borg",client.getPath());


        ApplicationResource applicationResource = client.org( "morg" ).getApp( "app" );
        assertNotNull( applicationResource );
        assertEquals( getBaseUri().toString()+"borg/morg",client.getPath());

        //        for(Entity entity: itr){
//
//        }
    }

    @Ignore
    public void stateful(){

        EntityResponse itr  =  client.org( "test" ).getApp( "test" ).users().getEntityResponse();

        for(Entity entity: itr){

        }
    }
}
