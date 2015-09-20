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


import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Tests endpoints that use /system/*
 */
public class SystemResourceIT extends AbstractRestIT {

    @Test
    public void testSystemDatabaseAlreadyRun() {
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "access_token", clientSetup.getSuperuserToken().getAccessToken() );

        Entity result = clientSetup.getRestClient().system().database().setup().put( queryParameters );

        assertNotNull( result );
        assertNotNull( "ok", ( String ) result.get( "status" ) );

        result = clientSetup.getRestClient().system().database().setup().put( queryParameters );

        assertNotNull( result );
        assertNotNull( "ok", ( String ) result.get( "status" ) );
    }

    @Test
    public void testDeleteAllApplicationEntities() throws Exception{
        int count = 10;
        for(int i =0; i<count;i++) {
            this.app().collection("tests").post(new Entity().chainPut("testval", "test"));
        }
        this.refreshIndex();
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam( "access_token", clientSetup.getSuperuserToken().getAccessToken() );
        queryParameters.addParam("confirmApplicationName", this.clientSetup.getAppName());

        org.apache.usergrid.rest.test.resource.model.ApiResponse result = clientSetup.getRestClient().system().getSubResource("applications/" + this.clientSetup.getAppUuid()).delete(false, queryParameters);

        assertNotNull(result);
        assertNotNull("ok", result.getStatus());
        assertNotNull(((LinkedHashMap) result.getData()).get("jobId"));

        String jobId = (String)((LinkedHashMap) result.getData()).get("jobId");
        queryParameters = new QueryParameters();
        for(int i = 0;i<10;i++ ) {
            result = clientSetup.getRestClient().system().applications(this.clientSetup.getAppUuid(), "job/" + jobId).get(queryParameters);
            String status = (String) ((LinkedHashMap) result.getData()).get("status");
            if(status.equals("COMPLETE")){
                break;
            }else{
                Thread.sleep(100);
            }
        }
        assertEquals(((LinkedHashMap) ((LinkedHashMap) result.getData()).get("metadata")).get("count"), 10);

    }


    @Test
    public void testBoostrapAlreadyRun() {
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam("access_token", clientSetup.getSuperuserToken().getAccessToken());

        Entity result = clientSetup.getRestClient().system().database().bootstrap().put(queryParameters);

        assertNotNull( result );
        assertNotNull( "ok", ( String ) result.get( "status" ) );

        result = clientSetup.getRestClient().system().database().bootstrap().put( queryParameters );

        assertNotNull( result );
        assertNotNull("ok", (String) result.get("status"));
    }
    @Test
    public void testQueueDepth() {
        QueryParameters queryParameters = new QueryParameters();
        queryParameters.addParam("access_token", clientSetup.getSuperuserToken().getAccessToken());

        Entity result = clientSetup.getRestClient().system().getSubResource("queue/size").get(Entity.class, queryParameters,false);

        assertNotNull( result );
        assertEquals(0, ((Map) result.get("data")).get("queueDepth"));


    }
}
