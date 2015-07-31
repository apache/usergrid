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
package org.apache.usergrid.rest.applications.events;


import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Collection;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Invoke application request counters
 *
 * @author realbeast
 */

public class ApplicationRequestCounterIT extends AbstractRestIT {
    private static final Logger log = LoggerFactory.getLogger( ApplicationRequestCounterIT.class );
    long ts = System.currentTimeMillis() - ( 24 * 60 * 60 * 1000 );

    @Test
    public void applicationrequestInternalCounters() throws Exception {

        //get default application
        ApiResponse defaultApp = org().app( clientSetup.getAppName() ).get();

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "resolution", "all" ).addParam( "counter", "application.requests" );
        Collection countersResponse = org().app( clientSetup.getAppName() ).collection( "counters" ).get( queryParameters ,true );

        assertNotNull( countersResponse );
        ArrayList counterValues = ( ArrayList ) countersResponse.getResponse().getProperties().get( "counters" );
        LinkedHashMap counters = ( LinkedHashMap ) counterValues.get( 0 );
        assertEquals( "application.requests", counters.get( "name" ) );

        //Since it was accessed twice above.
        assertEquals( 2, ( ( LinkedHashMap ) ( ( ArrayList)counters.get( "values" )).get( 0 )).get( "value" ));

    }
}
