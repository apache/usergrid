/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.rest;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.usergrid.rest.test.resource2point0.model.ApiResponse;
import org.apache.usergrid.rest.test.resource2point0.model.QueryParameters;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * test index creation
 */
public class IndexResourceIT extends org.apache.usergrid.rest.test.resource2point0.AbstractRestIT {

    @Rule
    public TestContextSetup context = new TestContextSetup( this );
    //Used for all MUUserResourceITTests
    private Logger LOG = LoggerFactory.getLogger(IndexResourceIT.class);

    public IndexResourceIT(){

    }

    @Test
    public void testCaseReindexEndpoint() {

        //Create Collection
        Map payload = new HashMap<>(  );
        payload.put( "name","wazer wifle!!" );
        ApiResponse collectionResponse = clientSetup.getRestClient()
            .pathResource( getOrgAppPath( "storelatlons" ) ).post( payload );

        assertNotNull( collectionResponse );

        //try reindex endpoint with ALl mixed case characters
        Token superUserToken = clientSetup.getRestClient().management().token().get(clientSetup.getSuperuserName(),clientSetup.getSuperuserPassword());

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "access_token",superUserToken.getAccessToken());
        ApiResponse result = clientSetup.getRestClient()
            .pathResource( "system/index/rebuild/"+clientSetup.getAppUuid()+"/StOrElaTloNs" )
            .post( false, ApiResponse.class, null, queryParameters, true );

        assertNotNull(result);

        //try the reindex endpoint with all lowercase Characters
        queryParameters = new QueryParameters();
        queryParameters.addParam( "access_token",clientSetup.getSuperuserToken().getAccessToken() );
        result = clientSetup.getRestClient()
            .pathResource( "system/index/rebuild/"+clientSetup.getAppUuid()+"/storelatlons" )
            .post( false, ApiResponse.class,null,queryParameters,true);

        assertNotNull( result );

    }

    @Ignore
    @Test
    public void TestAddIndex() throws Exception{


        Map<String, Object> data = new HashMap<String, Object>();
        data.put( "replicas", 0 );
        data.put( "shards", 1 );
        data.put( "writeConsistency", "one" );

        String appId = this.clientSetup.getAppUuid();

        // change the password as admin. The old password isn't required
        org.apache.usergrid.rest.test.resource2point0.model.ApiResponse node = null;
        try {

            WebResource resource = this.clientSetup.getRestClient().pathResource("/system/index/" + appId).getResource();

            //added httpBasicauth filter to all setup calls because they all do verification this way.
            HTTPBasicAuthFilter httpBasicAuthFilter = new HTTPBasicAuthFilter( clientSetup.getSuperuserName(),clientSetup.getSuperuserPassword() );
            resource.addFilter( httpBasicAuthFilter );

            node = resource.type( MediaType.APPLICATION_JSON_TYPE ).accept( MediaType.APPLICATION_JSON )
                .get( org.apache.usergrid.rest.test.resource2point0.model.ApiResponse.class);
        } catch (Exception e) {
            LOG.error("failed", e);
            fail(e.toString());
        }
        assertNotNull( node );
    }
}
