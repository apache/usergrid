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
package org.apache.usergrid.rest.applications.queries;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Rule;
import org.junit.Test;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.CustomCollection;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.apache.usergrid.utils.MapUtils.hashMap;


/**
 * @author tnine
 */
public class BadGrammarQueryTest extends AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );


    @Test
    public void catchBadQueryGrammar() throws IOException {

        CustomCollection things = context.customCollection( "things" );

        Map actor = hashMap( "displayName", "Erin" );
        Map props = new HashMap();
        props.put( "actor", actor );
        props.put( "content", "bragh" );

        JsonNode activity = things.create( props );

        refreshIndex(context.getOrgName(), context.getAppName());

        String query = "select * where name != 'go'";

        ClientResponse.Status status = null;

        try {

            JsonNode incorrectNode = things.query( query, "limit", Integer.toString( 10 ) );
            fail( "This should throw an exception" );
        }
        catch ( UniformInterfaceException uie ) {
             status = uie.getResponse().getClientResponseStatus();


        }

        assertEquals( 400, status.getStatusCode() );
    }
}
