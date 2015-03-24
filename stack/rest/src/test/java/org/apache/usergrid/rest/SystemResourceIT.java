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
package org.apache.usergrid.rest;


import org.junit.Test;

import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import static org.junit.Assert.assertNotNull;


/**
 * Tests endpoints that use /system/*
 */
public class SystemResourceIT extends AbstractRestIT {

    @Test
    public void testSystemDatabaseAlreadyRun() {
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "access_token",clientSetup.getSuperuserToken().getAccessToken() );

        Entity result = clientSetup.getRestClient().system().database().setup().get(queryParameters);

        assertNotNull(result);
        assertNotNull( "ok" ,(String)result.get( "status" ) );

        result = clientSetup.getRestClient().system().database().setup().get(queryParameters);

        assertNotNull( result );
        assertNotNull( "ok" ,(String)result.get( "status" ) );


    }

}
