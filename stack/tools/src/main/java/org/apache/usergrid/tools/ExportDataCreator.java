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


import java.util.UUID;

import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.Activity;
import org.apache.usergrid.persistence.entities.User;

import static org.junit.Assert.assertNotNull;


/**
 * Simple class to create test for for exporting
 *
 * @author tnine
 */
public class ExportDataCreator {

    private EntityManagerFactory emf;

    private ManagementService managementService;


    /**
     * @param emf
     * @param managementService
     */
    public ExportDataCreator( EntityManagerFactory emf, ManagementService managementService ) {
        super();
        this.emf = emf;
        this.managementService = managementService;
    }


    public void createTestData() throws Exception {

        String orgName = "testexportorg";

        //nothing to do 
        if ( managementService.getOrganizationByName( orgName ) != null ) {
            return;
        }

        OrganizationOwnerInfo orgInfo = managementService
                .createOwnerAndOrganization( orgName, "textExportUser@apigee.com", "Test User",
                        "textExportUser@apigee.com", "password", true, false );

        UUID appId = managementService.createApplication( orgInfo.getOrganization().getUuid(), "application" ).getId();

        EntityManager em = emf.getEntityManager( appId );

        User first = new User();
        first.setUsername( "first" );
        first.setEmail( "first@usergrid.com" );

        Entity firstUserEntity = em.create( first );

        assertNotNull( firstUserEntity );

        User second = new User();
        second.setUsername( "second" );
        second.setEmail( "second@usergrid.com" );

        Entity secondUserEntity = em.create( second );

        assertNotNull( secondUserEntity );

        em.createConnection( firstUserEntity, "likes", secondUserEntity );

        em.createConnection( secondUserEntity, "dislikes", firstUserEntity );

        // now create some activities and put them into the user stream

        Activity activity = new Activity();

        Activity.ActivityObject actor = new Activity.ActivityObject();
        actor.setEntityType( "user" );
        actor.setId( firstUserEntity.getUuid().toString() );

        activity.setActor( actor );
        activity.setVerb( "POST" );

        em.createItemInCollection( firstUserEntity, "activities", "activity", activity.getProperties() );
    }
}
