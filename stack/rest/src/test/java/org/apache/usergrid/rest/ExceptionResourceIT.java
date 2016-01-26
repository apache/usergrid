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


import java.util.Map;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ServerErrorException;

import org.junit.Test;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.test.resource.AbstractRestIT;
import org.apache.usergrid.rest.test.resource.model.Organization;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.services.exceptions.UnsupportedServiceOperationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ExceptionResourceIT extends AbstractRestIT{

    @Test
    public void testNonExistingEndpoint(){
        try {

            clientSetup.getRestClient()
                       .pathResource( getOrgAppPath( "non_existant_delete_endpoint" ) ).delete( );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }

    //test uncovered endpoints
    @Test
    public void testNotImplementedException(){
        try {

            clientSetup.getRestClient().management().orgs().delete( true );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }
    @Test
    public void testDeleteFromWrongEndpoint(){
        try {
            clientSetup.getRestClient()
                       .pathResource( clientSetup.getOrganizationName() + "/" + clientSetup.getAppName()  ).delete( );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }

    @Test
    public void testUnsupportedServiceOperation(){
        try {
            clientSetup.getRestClient()
                       .pathResource( getOrgAppPath( "users" ) ).delete( );
            fail("Should have thrown below exception");

        }catch(NotAllowedException e){
            assertEquals( 405,e.getResponse().getStatus());
        }
    }

}
