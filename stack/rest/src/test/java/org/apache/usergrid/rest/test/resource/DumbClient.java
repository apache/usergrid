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
package org.apache.usergrid.rest.test.resource;

import org.junit.Ignore;
import org.junit.Test;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;



/**
 * Test Class used to model if the client is working or doing what it is supposed to be doing.
 * ask if this is for test purposes and if so if I can mark it with junit
 */

public class DumbClient extends AbstractRestIT {

    @Test
    public void stuff(){

        String name = "stuff"+ UUIDUtils.newTimeUUID();
       // User user = new User( "derp","derp", "derp"  );


        //Organization org = clientSetup.getRestClient().management().orgs().post(  )
      //  clientSetup.getRestClient().management().orgs().delete(org.getName);
       // OrganizationResource response =  clientSetup.getRestClient().management().orgs().organization( "" );
        //assertNotNull( response );
        //EntityResponse itr  =  client.org( "test" ).getApp( "test" ).users().getEntityResponse();
        //for(Entity entity: itr){
    }


    @Ignore
    public void stateful(){

//        EntityResponse itr  =  client.org( "test" ).getApp( "test" ).users().getEntityResponse();
//
//        for(Entity entity: itr){
//
//        }
    }
}
