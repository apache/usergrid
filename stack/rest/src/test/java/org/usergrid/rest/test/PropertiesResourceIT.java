/*
 * Copyright 2013 ApigeeCorporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.usergrid.rest.test;


import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.rest.AbstractRestIT;
import org.usergrid.services.ServiceManagerFactory;

import static org.usergrid.management.AccountCreationProps.PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION;


public class PropertiesResourceIT extends AbstractRestIT {
    static final Logger logger = LoggerFactory.getLogger( PropertiesResourceIT.class );

    @Autowired
    protected ServiceManagerFactory smf;


    @Test
    public void testBasicOperation() {

        // set property locally
        setup.getMgmtSvc().getProperties().put( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );

        // verify that it is set locally
        Assert.assertTrue( setup.getMgmtSvc().newAdminUsersRequireConfirmation() );

        // verify that is is not set in Jetty
        {
            Map<String, String> map = resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( Map.class );
            Assert.assertFalse( Boolean.parseBoolean( map.get( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION ) ) );
        }

        // set property in Jetty
        {
            Map<String, String> props = new HashMap<String, String>();
            props.put( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION, "true" );
            resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).post( props );
        }

        // verify that it is set in Jetty
        {
            Map<String, String> map = resource().path( "/testproperties" ).queryParam( "access_token", access_token )
                    .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( Map.class );

            Assert.assertTrue( Boolean.parseBoolean( map.get( PROPERTIES_ADMIN_USERS_REQUIRE_CONFIRMATION ) ) );
        }
    }
}
