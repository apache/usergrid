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

import org.apache.usergrid.ExperimentalTest;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.corepersistence.index.ReIndexService.Status;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * test index creation
 */
public class IndexResourceIT extends AbstractRestIT {

    //Used for all MUUserResourceITTests
    private static final Logger LOG = LoggerFactory.getLogger(IndexResourceIT.class);

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
        Token superUserToken = clientSetup.getRestClient().management().token()
            .get(clientSetup.getSuperuserName(),clientSetup.getSuperuserPassword());

        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam("access_token", superUserToken.getAccessToken());
        ApiResponse result = clientSetup.getRestClient()
            .pathResource("system/index/rebuild/" + clientSetup.getAppUuid() + "/StOrElaTloNs")
            .post(false, ApiResponse.class, null, queryParameters, true);

        assertNotNull(result);
        assertEquals(Status.STARTED.name(), result.getStatus());

        //try the reindex endpoint with all lowercase Characters
        queryParameters = new QueryParameters();
        queryParameters.addParam("access_token", superUserToken.getAccessToken());
        result = clientSetup.getRestClient()
            .pathResource("system/index/rebuild/" + clientSetup.getAppUuid() + "/storelatlons")
            .post(false, ApiResponse.class, null, queryParameters, true);
        
        assertNotNull(result);
        //at this point, this could return a result of the previous reindex, or if it has completed, it will create a new job
        assertNotEquals(Status.UNKNOWN.name(), result.getStatus());

        WebTarget res = clientSetup.getRestClient()
            .pathResource("system/index/rebuild/" + clientSetup.getAppUuid() + "/storelatlons")
            .getTarget();

        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
            .credentials( "superuser", "superpassword" ).build();

        result = res.register( feature ).request().get( ApiResponse.class );
        assertNotNull(result);
        
        int retry = 0;
        while(retry < 5 && !result.getStatus().equals(Status.COMPLETE.name())) {
        	try {
        		//hope reindex completes, if not, that's still ok
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				
			}
        	result = res.register( feature ).request().get( ApiResponse.class );
        	retry++;
        	assertNotNull(result);
        }
        
        Map<String, Object> resultMap = result.getProperties();
        assertNotNull( resultMap );
        assertEquals(1,resultMap.get("numberQueued"));


    }

    @Test
    @Category(ExperimentalTest.class)
    public void TestAddIndex() throws Exception{


        Map<String, Object> data = new HashMap<String, Object>();
        data.put( "replicas", 0 );
        data.put( "shards", 1 );
        data.put( "writeConsistency", "one" );

        String appId = this.clientSetup.getAppUuid();

        // change the password as admin. The old password isn't required
        org.apache.usergrid.rest.test.resource.model.ApiResponse node = null;
        try {

            WebTarget resource = this.clientSetup.getRestClient().pathResource("/system/index/" + appId).getTarget();

            HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .credentials( "superuser", "superpassword" ).build();

            node = resource.register( feature ).request()
                .accept( MediaType.APPLICATION_JSON )
                .get( org.apache.usergrid.rest.test.resource.model.ApiResponse.class);

        } catch (Exception e) {
            LOG.error("failed", e);
            fail(e.toString());
        }
        assertNotNull( node );
    }
}
