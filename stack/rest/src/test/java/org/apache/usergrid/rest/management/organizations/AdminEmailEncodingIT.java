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
package org.apache.usergrid.rest.management.organizations;


import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.security.TestAdminUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Tests for admin emails with + signs create accounts correctly, and can get tokens in both the POST and GET forms of
 * the api
 *
 * @author tnine
 */
@Concurrent()
public class AdminEmailEncodingIT extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void getTokenPlus() throws Exception {
        String org = "AdminEmailEncodingTestgetTokenPlus";
        String app = "Plus";

        doTest( "+", org, app );
    }


    @Test
    public void getTokenUnderscore() throws Exception {
        String org = "AdminEmailEncodingTestgetTokenUnderscore";
        String app = "Underscore";

        doTest( "_", org, app );
    }


    @Test
    public void getTokenDash() throws Exception {
        String org = "AdminEmailEncodingTestgetTokenDash";
        String app = "Dash";

        doTest( "-", org, app );
    }


    private void doTest( String symbol, String org, String app ) throws IOException {

        org = org.toLowerCase();
        app = app.toLowerCase();

        String email = String.format( "admin%sname@adminemailencodingtest.org", symbol );
        String user = email;
        String password = "password";

        TestAdminUser adminUser = new TestAdminUser( user, password, email );

        context.withApp( app ).withOrg( org ).withUser( adminUser );

        // create the org and app
        context.createNewOrgAndUser();

        // no need for refresh here as Service module does an index refresh when org/app created

        // now log in via a GET

        String getToken = context.management().tokenGet( email, password );

        assertNotNull( getToken );

        String postToken = context.management().tokenPost( email, password );

        assertNotNull( postToken );

        // not log in with our admin
        context.withUser( adminUser ).loginUser();

        //now get the "me" and ensure it's correct

        JsonNode data = context.management().me().get();

        assertNotNull( data.get( "access_token" ).asText() );

        data = context.management().users().user( email ).get();

        JsonNode admin = data.get( "data" ).get( "organizations" ).get( org ).get( "users" ).get( email );

        assertNotNull( admin );

        assertEquals( email, admin.get( "email" ).asText() );
        assertEquals( user, admin.get( "username" ).asText() );
    }
}
