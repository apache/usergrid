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
package org.apache.usergrid.services;


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.annotation.NotThreadSafe;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.createTimestamp;
import static org.apache.usergrid.persistence.cassandra.CassandraService.DEFAULT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.services.ServiceParameter.filter;
import static org.apache.usergrid.services.ServiceParameter.parameters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


@NotThreadSafe()
public class ServiceRequestIT {

    private static final Logger logger = LoggerFactory.getLogger( ServiceRequestIT.class );

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @Rule
    public ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );


    @Test
    public void testPaths() throws Exception {

        UUID applicationId = DEFAULT_APPLICATION_ID;

        ServiceManager services = setup.getSmf().getServiceManager( applicationId );

        ServiceRequest path = services.newRequest( ServiceAction.GET, parameters( "users", "bob" ), null );
        // path = path.addSegment("users", "bob");
        logger.info( "" + path.getParameters() );

        Map<List<String>, List<String>> replaceParameters = new LinkedHashMap<List<String>, List<String>>();
        replaceParameters.put( Arrays.asList( "users" ), Arrays.asList( "connecting", "users" ) );
        List<ServiceParameter> p = filter( path.getParameters(), replaceParameters );
        // path = path.addSegment("messages", "bob");
        logger.info( "" + p );

        path = services.newRequest( ServiceAction.GET, parameters( "users", UUID.randomUUID(), "messages" ), null );
        logger.info( "" + path.getParameters() );

        logger.info( "\\1" );
        replaceParameters = new LinkedHashMap<List<String>, List<String>>();
        replaceParameters.put( Arrays.asList( "users", "$id" ), Arrays.asList( "connecting", "\\1", "users" ) );
        p = filter( path.getParameters(), replaceParameters );
        logger.info( "" + p );
    }

    //Verify that entity read repair is functioning as intended.
    @Test
    public void testRepairOfSingleEntity() throws Exception{
        String rand = RandomStringUtils.randomAlphanumeric( 10 );

        String orgName = "org_" + rand;
        String appName = "app_" +rand;
        String username = "username_" + rand;
        String adminUsername = "admin_"+rand;
        String email = username+"@derp.com";
        String password = username;

        String collectionName = "users";


        OrganizationOwnerInfo organizationOwnerInfo = setup.getMgmtSvc().createOwnerAndOrganization( orgName,adminUsername,adminUsername,email,password );

        ApplicationInfo applicationInfo = setup.getMgmtSvc().createApplication( organizationOwnerInfo.getOrganization().getUuid(),appName );

        EntityManager entityManager = setup.getEmf().getEntityManager( applicationInfo.getId() );

        Map<String,Object> userInfo = new HashMap<String, Object>(  );
        userInfo.put( "username",username );

        //Entity entityToBeCorrupted = entityManager.create( collectionName,userInfo );

        CassandraService cass = setup.getCassSvc();

        Object key = CassandraPersistenceUtils.key( applicationInfo.getId(), collectionName, "username", username );

        Keyspace ko = cass.getApplicationKeyspace( applicationInfo.getId() );
        Mutator<ByteBuffer> m = createMutator( ko, be );

        UUID testEntityUUID = UUIDUtils.newTimeUUID();
        //this below calll should make the column family AND the column name
        addInsertToMutator( m,ENTITY_UNIQUE,key, testEntityUUID,0,createTimestamp());

        m.execute();

        //verify that there is no corresponding entity with the uuid or alias provided
        //verify it returns null.
        assertNull(entityManager.get( testEntityUUID ));

        //the below works but not needed for this test.
        //assertNull( entityManager.getAlias("user",username));

        //verify that we cannot recreate the entity due to duplicate unique property exception
        Entity entityToBeCorrupted = null;
        try {
            entityToBeCorrupted = entityManager.create( collectionName, userInfo );
        }catch(Exception e){
            fail("Create call should have worked");
        }

        assertNotNull( entityToBeCorrupted );
        assertEquals( username,( ( User ) entityToBeCorrupted ).getUsername() );

    }


    @Test
    public void testRepairOfOnlyOneOfTwoColumns() throws Exception{
        String rand = RandomStringUtils.randomAlphanumeric( 10 );

        String orgName = "org_" + rand;
        String appName = "app_" +rand;
        String username = "username_" + rand;
        String adminUsername = "admin_"+rand;
        String email = username+"@derp.com";
        String password = username;

        String collectionName = "users";


        OrganizationOwnerInfo organizationOwnerInfo = setup.getMgmtSvc().createOwnerAndOrganization( orgName,adminUsername,adminUsername,email,password );

        ApplicationInfo applicationInfo = setup.getMgmtSvc().createApplication( organizationOwnerInfo.getOrganization().getUuid(),appName );

        EntityManager entityManager = setup.getEmf().getEntityManager( applicationInfo.getId() );

        Map<String,Object> userInfo = new HashMap<String, Object>(  );
        userInfo.put( "username",username );

        //Entity entityToBeCorrupted = entityManager.create( collectionName,userInfo );

        CassandraService cass = setup.getCassSvc();

        Object key = CassandraPersistenceUtils.key( applicationInfo.getId(), collectionName, "username", username );

        Keyspace ko = cass.getApplicationKeyspace( applicationInfo.getId() );
        Mutator<ByteBuffer> m = createMutator( ko, be );

        //create a new column
        Entity validCoexistingEntity = entityManager.create( collectionName, userInfo );

        UUID testEntityUUID = UUIDUtils.newTimeUUID();
        //this below calll should make the column family AND the column name for an already existing column thus adding one legit and one dummy value.
        addInsertToMutator( m,ENTITY_UNIQUE,key, testEntityUUID,0,createTimestamp());

        m.execute();

        //verify that there is no corresponding entity with the uuid or alias provided
        //verify it returns null.
        assertNull(entityManager.get( testEntityUUID ));

        //the below works but not needed for this test.
        //assertNull( entityManager.getAlias("user",username));

        //verify that we cannot recreate the entity due to duplicate unique property exception
        Entity entityToBeCorrupted = null;
        try {
            entityToBeCorrupted = entityManager.create( collectionName, userInfo );
            fail();
        }catch(DuplicateUniquePropertyExistsException dup){

        }
        catch(Exception e){
            throw e;
        }

        //verifies it works now.
        assertNotNull( entityManager
                .get( entityManager.getAlias( applicationInfo.getId(), collectionName, username ).getUuid() ) );

    }

    //For this test you need to insert a dummy key with a dummy column that leads to nowhere
    //then run the unique index cleanup.
    //checks for bug when only column doesn't exist make sure to delete the row as well.
    @Test
    public void testRepairOfMultipleEntitiesAndRemainingEntities() throws Exception{
        String rand = RandomStringUtils.randomAlphanumeric( 10 );

        int numberOfEntitiesToCreate = 1000;

        String orgName = "org_" + rand;
        String appName = "app_" +rand;
        String username = "username_" + rand;
        String adminUsername = "admin_"+rand;
        String email = username+"@derp.com";
        String password = username;

        String collectionName = "users";


        OrganizationOwnerInfo organizationOwnerInfo = setup.getMgmtSvc().createOwnerAndOrganization( orgName,adminUsername,adminUsername,email,password );

        ApplicationInfo applicationInfo = setup.getMgmtSvc().createApplication( organizationOwnerInfo.getOrganization().getUuid(),appName );

        EntityManager entityManager = setup.getEmf().getEntityManager( applicationInfo.getId() );

        String[] usernames = new String[numberOfEntitiesToCreate];
        Entity[] entities = new Entity[numberOfEntitiesToCreate];

        int index = 0;
        while(index < numberOfEntitiesToCreate) {

            usernames[index]=username+index;

            Map<String, Object> userInfo = new HashMap<String, Object>();
            userInfo.put( "username", usernames[index] );

            CassandraService cass = setup.getCassSvc();

            entities[index] = entityManager.create( collectionName,userInfo );

            Object key = CassandraPersistenceUtils.key( applicationInfo.getId(), collectionName, "username", usernames[index] );

            Keyspace ko = cass.getApplicationKeyspace( applicationInfo.getId() );
            Mutator<ByteBuffer> m = createMutator( ko, be );

            UUID testEntityUUID = UUIDUtils.newTimeUUID();
            //this below calll should make the column family AND the column name
            addInsertToMutator( m, ENTITY_UNIQUE, key, testEntityUUID, 0, createTimestamp() );

            m.execute();
            index++;

            //the below works but not needed for this test.
            //assertNull( entityManager.getAlias("user",username));

            //verify that we cannot recreate the entity due to duplicate unique property exception
            Entity entityToBeCorrupted = null;
            try {
                entityToBeCorrupted = entityManager.create( collectionName, userInfo );
                fail();
            }catch(DuplicateUniquePropertyExistsException dup){

            }
            catch(Exception e){
                fail("shouldn't throw something else i think");
            }

        }


        for(String user:usernames ) {
            Map<String, Object> userInfo = new HashMap<String, Object>();
            userInfo.put( "username", user);


            EntityRef entityRef = entityManager.getAlias( collectionName,user );

            Entity entityToBeCorrupted = entityManager.get( entityRef );
            assertNotNull( entityToBeCorrupted );
            assertEquals( user, ( ( User ) entityToBeCorrupted ).getUsername() );
        }
    }
}
