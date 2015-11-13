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


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.RandomStringUtils;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.createTimestamp;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


/**
 * Created by ApigeeCorporation on 11/2/15.
 */
public class UserUniqueIndexCleanupTest {
    static final Logger logger = LoggerFactory.getLogger( ExportAppTest.class );

    int NUM_COLLECTIONS = 10;
    int NUM_ENTITIES = 50;
    int NUM_CONNECTIONS = 3;

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );


    @org.junit.Test
    public void testBasicOperation() throws Exception {
        UserUniqueIndexCleanup userUniqueIndexCleanup = new UserUniqueIndexCleanup();
        userUniqueIndexCleanup.startTool( new String[] {
                "-host", "localhost:9160"
        }, false );

        System.out.println( "completed" );
    }

    //this test is perfect for the other tool the userCollectionFix tool as this is what I believe they were seeing.
    @Ignore ("WRong test not made for unique index cleanup.")
    public void testRepairOfSingleEntityMissingColumnWrongTool() throws Exception{
        String rand = RandomStringUtils.randomAlphanumeric( 10 );

        String orgName = "org_" + rand;
        String appName = "app_" +rand;
        String username = "username_" + rand;
        String email = username+"@derp.com";
        String password = username;

        String collectionName = "users";


        OrganizationOwnerInfo organizationOwnerInfo = setup.getMgmtSvc().createOwnerAndOrganization( orgName,username,username,email,password );

        ApplicationInfo applicationInfo = setup.getMgmtSvc().createApplication( organizationOwnerInfo.getOrganization().getUuid(),appName );

        EntityManager entityManager = setup.getEmf().getEntityManager( applicationInfo.getId() );

        Map<String,Object> userInfo = new HashMap<String, Object>(  );
        userInfo.put( "username",username );

        Entity entityToBeCorrupted = entityManager.create( collectionName,userInfo );

        Object key = CassandraPersistenceUtils.key( applicationInfo.getId(), collectionName, "username", username );
        CassandraService cass = setup.getCassSvc();

        List<HColumn<ByteBuffer, ByteBuffer>> cols =
                cass.getColumns( cass.getApplicationKeyspace( applicationInfo.getId() ), ENTITY_UNIQUE, key, null, null,
                        2, false );

        Set<UUID> results = new HashSet<UUID>( cols.size() );

        for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
            results.add( ue.fromByteBuffer( col.getName() ) );
        }

        UUID uuid = results.iterator().next();

        UUID timestampUuid = newTimeUUID();
        long timestamp = getTimestampInMicros( timestampUuid );

        //Keyspace ko = cass.getUsergridApplicationKeyspace();
        Keyspace ko = cass.getApplicationKeyspace( applicationInfo.getId() );
        Mutator<ByteBuffer> m = createMutator( ko, be );

        key = key( applicationInfo.getId(), collectionName, "username", username );
        //addDeleteToMutator( m, ENTITY_UNIQUE, key, uuid, timestamp );
        addDeleteToMutator( m, ENTITY_UNIQUE, key, timestamp, uuid );
        m.execute();

        assertNull( entityManager.getAlias( applicationInfo.getId(), collectionName, username ) );

        assertNotNull(entityManager.get( entityToBeCorrupted.getUuid() ));

        //run the cleanup
        UserUniqueIndexCleanup userUniqueIndexCleanup = new UserUniqueIndexCleanup();
        userUniqueIndexCleanup.startTool( new String[] {
                "-host", "localhost:"+ ServiceITSuite.cassandraResource.getRpcPort()
        }, false );


        //here you need to add a delete to the mutator then recheck it and see if the entity is the same as .
        Thread.sleep( 2000 );
        assertNull( entityManager.get( entityToBeCorrupted.getUuid() ) );

        //When you come back you also need to emulate the tests to delete what is out of the uuid without any corresponding data.
        //maybe it'll be easier to just do an insert into the EntityUnique row without doint it into any other row and
        //then verify the data like that. Then you don't have to do deletes out of other things.

    }

    //For this test you need to insert a dummy key with a dummy column that leads to nowhere
    //then run the unique index cleanup.
    //checks for bug when only column doesn't exist make sure to delete the row as well.

    //due to the read repair this is no longer a valid test of hte unique index cleanup
    @Ignore
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
        //assertNull(entityManager.get( testEntityUUID ));

        //verify that we cannot recreate the entity due to duplicate unique property exception
        //The Get above should have repaired the entity allowing it run
        Entity entityToBeCorrupted = null;
        try {
            entityToBeCorrupted = entityManager.create( collectionName, userInfo );
            //fail();
        }catch(DuplicateUniquePropertyExistsException dup){
            fail();
        }
        catch(Exception e){
            fail("shouldn't throw something else i think");
        }


        //run the cleanup
//        UniqueIndexCleanup uniqueIndexCleanup = new UniqueIndexCleanup();
//        uniqueIndexCleanup.startTool( new String[] {
//                "-host", "localhost:"+ ServiceITSuite.cassandraResource.getRpcPort()
//        }, false );


        entityToBeCorrupted = entityManager.create( collectionName,userInfo );

        assertNotNull( entityToBeCorrupted );
        assertEquals( username,( ( User ) entityToBeCorrupted ).getUsername());

    }

    //For this test you need to insert a dummy key with a dummy column that leads to nowhere
    //then run the unique index cleanup.
    //checks for bug when only column doesn't exist make sure to delete the row as well.
   // Due to the read repair this is no longer a valid test of unique index cleanup.
    @Test
    @Ignore
    public void testRepairOfMultipleEntities() throws Exception{
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

        int index = 0;
        while(index < numberOfEntitiesToCreate) {

            usernames[index]=username+index;

            Map<String, Object> userInfo = new HashMap<String, Object>();
            userInfo.put( "username", usernames[index] );

            CassandraService cass = setup.getCassSvc();

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

        //run the cleanup
        UserUniqueIndexCleanup userUniqueIndexCleanup = new UserUniqueIndexCleanup();
        userUniqueIndexCleanup.startTool( new String[] {
                "-host", "localhost:"+ ServiceITSuite.cassandraResource.getRpcPort()
        }, false );

        for(String user:usernames ) {
            Map<String, Object> userInfo = new HashMap<String, Object>();
            userInfo.put( "username", user);
            Entity entityToBeCorrupted = entityManager.create( collectionName, userInfo );

            assertNotNull( entityToBeCorrupted );
            assertEquals( user, ( ( User ) entityToBeCorrupted ).getUsername() );
        }
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


        //run the cleanup
        UserUniqueIndexCleanup userUniqueIndexCleanup = new UserUniqueIndexCleanup();
        userUniqueIndexCleanup.startTool( new String[] {
                "-host", "localhost:"+ ServiceITSuite.cassandraResource.getRpcPort()
        }, false );

        //verifies it works now.
        assertNotNull( entityManager
                .get( entityManager.getAlias( applicationInfo.getId(), collectionName, username ).getUuid() ) );

    }

    @Test
    public void testStringParsing(){
        UserUniqueIndexCleanup userUniqueIndexCleanup = new UserUniqueIndexCleanup();

        //String garbageString = ")xƐ:�^Q?�I�p\\�/2178c690-3a6f-11e4-aec6-48fa705cb26f:users:username:test";

        UUID uuid = UUIDUtils.newTimeUUID();

        String garbageString = "S2^? >-^Q��%\"�]^S:"+uuid+":users:username:2";

        String[] parsedString = garbageString.split( ":" );

        String[] cleanedString = userUniqueIndexCleanup.garbageRowKeyParser( parsedString );

        assertEquals( uuid.toString(),cleanedString[0] );
        assertEquals( "users",cleanedString[1] );
        assertEquals( "username",cleanedString[2] );
        assertEquals( "2",cleanedString[3] );
    }

    //POinting at single values is broken now but not entirely used right now anyways.
    //@Ignore
    @Test
    public void testRepairOfOnlyOneOfTwoColumnsWhilePointingAtSingleValue() throws Exception{
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

        //verify that we cannot recreate the entity due to duplicate unique property exception
        Entity entityToBeCorrupted = null;
        try {
            entityToBeCorrupted = entityManager.create( collectionName, userInfo );
            fail();
        }catch(DuplicateUniquePropertyExistsException dup){

        }
        catch(Exception e) {
            fail( "shouldn't throw something else i think" );
        }

        //NEED TO FAIL MORE GRACEFULLY
        //run the cleanup
        UserUniqueIndexCleanup userUniqueIndexCleanup = new UserUniqueIndexCleanup();
        userUniqueIndexCleanup.startTool( new String[] {
                "-host", "localhost:"+ ServiceITSuite.cassandraResource.getRpcPort(),
                "-col",collectionName,
                "-app",applicationInfo.getId().toString(),
                "-property","username",
                "-value",username
        }, false );

        //verifies it works now.
        assertNotNull( entityManager
                .get( entityManager.getAlias( applicationInfo.getId(), collectionName, username ).getUuid() ) );

    }
}

