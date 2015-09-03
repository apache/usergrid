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


import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.ws.rs.ClientErrorException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class BasicIT extends AbstractRestIT {

    private static final Logger LOG = LoggerFactory.getLogger( BasicIT.class );


    public BasicIT() throws Exception {
        super();
    }

    /**
     * For USERGRID-2099 where putting an entity into a generic collection is resulting in a CCE when the name is a UUID
     * string.
     */
    @Test
    public void testGenericCollectionEntityNameUuid() throws Exception {

        String uuid = UUIDUtils.newTimeUUID().toString();

        // Notice for 'name' we replace the dash in uuid string
        // with 0's making it no longer conforms to a uuid
        Entity payload = new Entity();
        payload.put( "name", uuid.replace( '-', '0' ) );

        //Create a user with the payload
        Entity returnedUser = this.app().collection( "suspects" ).post(payload);
        assertNotNull( returnedUser );

        // Now this should pass with the corrections made to USERGRID-2099 which
        // disables conversion of uuid strings into UUID objects in JsonUtils
        payload.put( "name", uuid );

        returnedUser = this.app().collection( "suspects" ).post(payload);

        assertNotNull( returnedUser );
    }

    @Test
    public void serviceResourceNotFoundReturns404() throws Exception {

            try {
                clientSetup.getRestClient().pathResource( getOrgAppPath( "users/JOE" ) ).get( ApiResponse.class );
                fail("A get on a nonexistant object should fail");
            } catch ( ClientErrorException e ) {
                assertEquals( "Guests should not be able to get a 404", 404,
                    e.getResponse().getStatusInfo().getStatusCode() );
            }
    }
}
