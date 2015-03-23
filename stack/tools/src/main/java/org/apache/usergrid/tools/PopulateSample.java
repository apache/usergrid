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
package org.apache.usergrid.tools;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.Activity;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceRequest;
import org.apache.usergrid.services.ServiceResults;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.apache.usergrid.services.ServiceParameter.parameters;
import static org.apache.usergrid.services.ServicePayload.payload;


public class PopulateSample extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( PopulateSample.class );


    @Override
    public Options createOptions() {

        Option useSpring = OptionBuilder.create( "spring" );

        Options options = new Options();
        options.addOption( useSpring );

        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {
        logger.info( "Starting test..." );
        startSpring();

        UserInfo user = managementService.createAdminUser( "admin", "admin", "admin@ug.com", "none", false, false );

        logger.info( "Creating organization: sample-organization" );
        // management
        // .createOrganization("sample-organization", "sample@organization.com",
        // "1234");
        OrganizationInfo organization = managementService.createOrganization( "sample-organization", user, false );

        logger.info( "creating application: testEntityManagerTest" );
        // TODO update to organizationName/applicationName
        UUID applicationId =
                managementService.createApplication( organization.getUuid(), "sample-application" ).getId();

        ServiceManager sm = smf.getServiceManager( applicationId );

        EntityManager em = emf.getEntityManager( applicationId );

        // Create user
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff" );
        properties.put( "email", "ed@anuff.com" );
        properties.put( "name", "Ed Anuff" );

        Entity user1 = em.create( "user", properties );

        // Create activity
        properties =
                Activity.newActivity( Activity.VERB_POST, null, "I ate a sammich", null, user1, null, "tweet", null,
                        null ).getProperties();

        @SuppressWarnings("unused") Entity activity =
                testRequest( sm, ServiceAction.POST, 1, properties, "users", user1.getUuid(), "activities" )
                        .getEntity();

        // Create another activity
        properties = Activity.newActivity( Activity.VERB_POST, null, "cool pic dude", null, user1, null, "tweet", null,
                null ).getProperties();

        activity = testRequest( sm, ServiceAction.POST, 1, properties, "users", user1.getUuid(), "activities" )
                .getEntity();

        // Create another user
        properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "justin" );
        properties.put( "email", "justin@gmail.com" );
        properties.put( "name", "Justin Clark" );

        Entity user2 = em.create( "user", properties );

        // Create activity
        properties =
                Activity.newActivity( Activity.VERB_POST, null, "ATT U-verse May payment", null, user2, null, "payment",
                        null, null ).getProperties();

        activity = testRequest( sm, ServiceAction.POST, 1, properties, "users", user2.getUuid(), "activities" )
                .getEntity();

        // Connections
        em.createConnection( user1, "workWith", user2 );
    }


    public ServiceResults testRequest( ServiceManager sm, ServiceAction action, int expectedCount,
                                       Map<String, Object> properties, Object... params ) throws Exception {
        ServiceRequest request = sm.newRequest( action, parameters( params ), payload( properties ) );

        logger.info( "Request: " + action + " " + request.toString() );

        ServiceResults results = request.execute();
        assertNotNull( results );
        assertEquals( expectedCount, results.getEntities().size() );

        return results;
    }
}
