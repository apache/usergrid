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

package org.apache.usergrid.management.importer;

import com.amazonaws.SDKGlobalConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Module;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.services.notifications.QueueListener;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


//@Concurrent
public class ImportCollectionIT {

    private static final Logger logger = LoggerFactory.getLogger(ImportCollectionIT.class);


    private static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();

    // app-level data generated only once
    private static UserInfo adminUser;
    private static OrganizationInfo organization;
    private static UUID applicationId;

    QueueListener listener;


    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static final ServiceITSetup setup =
        new ServiceITSetupImpl( cassandraResource, new ElasticSearchResource() );


    @BeforeClass
    public static void setup() throws Exception {
        String username = "test"+ UUIDUtils.newTimeUUID();

        // start the scheduler after we're all set up
        JobSchedulerService jobScheduler = cassandraResource.getBean( JobSchedulerService.class );
        if ( jobScheduler.state() != Service.State.RUNNING ) {
            jobScheduler.startAndWait();
        }

        //creates sample test application
        adminUser = setup.getMgmtSvc().createAdminUser(
            username, username, username+"@test.com", username, false, false );
        organization = setup.getMgmtSvc().createOrganization( username, adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), username+"app" ).getId();
    }


    @Before
    public void before() {

        boolean configured =
                   !StringUtils.isEmpty(System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR))
                && !StringUtils.isEmpty(System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR))
                && !StringUtils.isEmpty(System.getProperty("bucketName"));

        if ( !configured ) {
            logger.warn("Skipping test because {}, {} and bucketName not " +
                "specified as system properties, e.g. in your Maven settings.xml file.",
                new Object[] {
                    SDKGlobalConfiguration.SECRET_KEY_ENV_VAR,
                    SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR
                });
        }

        Assume.assumeTrue( configured );
   }

    // test case to check if a collection file is imported correctly
    @Test
    public void testExportImportCollection() throws Exception {


        // create a collection of "thing" entities in the first application, export to S3

        final EntityManager emApp1 = setup.getEmf().getEntityManager( applicationId );
        Map<UUID, Entity> thingsMap = new HashMap<>();
        List<Entity> things = new ArrayList<>();
        createTestEntities(emApp1, thingsMap, things, "thing");

        deleteBucket();
        exportCollection( emApp1, "things" );


        // create new second application, import the data from S3

        final UUID appId2 = setup.getMgmtSvc().createApplication(
            organization.getUuid(), "second").getId();

        final EntityManager emApp2 = setup.getEmf().getEntityManager(appId2);
        importCollection( emApp2, "things" );


        // make sure that it worked

        try {
            logger.debug("\n\nCheck connections\n");

            List<Entity> importedThings = emApp2.getCollection(
                appId2, "things", null, Level.ALL_PROPERTIES).getEntities();
            assertTrue( !importedThings.isEmpty() );

            // two things have connections

            int conCount = 0;
            for ( Entity e : importedThings ) {
                Results r = emApp2.getConnectedEntities( e, "related", null, Level.IDS);
                List<ConnectionRef> connections = r.getConnections();
                conCount += connections.size();
            }
            assertEquals( 2, conCount );

            logger.debug("\n\nCheck dictionaries\n");

            // first two items have things in dictionary

            EntityRef entity0 = importedThings.get(0);
            Map connected0 = emApp2.getDictionaryAsMap(entity0, "connected_types");
            Map connecting0 = emApp2.getDictionaryAsMap(entity0, "connected_types");
            Assert.assertEquals( 1, connected0.size() );
            Assert.assertEquals( 1, connecting0.size() );

            EntityRef entity1 = importedThings.get(1);
            Map connected1 = emApp2.getDictionaryAsMap(entity1, "connected_types");
            Map connecting1 = emApp2.getDictionaryAsMap(entity1, "connected_types");
            Assert.assertEquals( 1, connected1.size() );
            Assert.assertEquals( 1, connecting1.size() );

            // rest do not

            EntityRef entity2 = importedThings.get(2);
            Map connected2 = emApp2.getDictionaryAsMap(entity2, "connected_types");
            Map connecting2 = emApp2.getDictionaryAsMap(entity2, "connected_types");
            Assert.assertEquals( 0, connected2.size() );
            Assert.assertEquals( 0, connecting2.size() );

            // if entities are deleted from app1, they still exist in app2

            logger.debug("\n\nCheck dictionary\n");
            for ( Entity importedThing : importedThings ) {
                emApp1.delete( importedThing );
            }
            emApp1.refreshIndex();
            emApp2.refreshIndex();

            importedThings = emApp2.getCollection(
                appId2, "things", null, Level.ALL_PROPERTIES).getEntities();
            assertTrue( !importedThings.isEmpty() );

        }
        finally {
            deleteBucket();
        }
    }


    /**
     * Test that an existing collection of entities can be updated
     * by doing an import of entities identified by UUIDs.
     */
    @Test
    public void testUpdateByImport() throws Exception {


        // create collection of things in first application, export them to S3

        final EntityManager emApp1 = setup.getEmf().getEntityManager( applicationId );

        Map<UUID, Entity> thingsMap = new HashMap<>();
        List<Entity> things = new ArrayList<>();
        createTestEntities(emApp1, thingsMap, things, "thing");

        deleteBucket();
        exportCollection( emApp1, "things" );


        // create new second application and import those things from S3

        final UUID appId2 = setup.getMgmtSvc().createApplication(
            organization.getUuid(), "second").getId();

        final EntityManager emApp2 = setup.getEmf().getEntityManager(appId2);
        importCollection( emApp2, "things" );


        // update the things in the second application, export to S3

        for ( UUID uuid : thingsMap.keySet() ) {
            Entity entity = emApp2.get( uuid );
            entity.setProperty("fuel_source", "Hydrogen");
            emApp2.update( entity );
        }

        deleteBucket();
        exportCollection( emApp2, "things" );


        // import the updated things back into the first application, check that they've been updated

        importCollection( emApp1, "things" );

        for ( UUID uuid : thingsMap.keySet() ) {
            Entity entity = emApp1.get( uuid );
            Assert.assertEquals("Hydrogen", entity.getProperty("fuel_source"));
        }
    }


    /**
     * Test that an existing collection of entities can be updated
     * by doing an import of entities identified by UUIDs.
     */
    @Test
    public void testCreateByImport() {

        // import from a JSON file stored locally on disk, with no UUID or type info

        // check that entities were created

    }


    /**
     * Test that the types of incoming entities is ignored.
     */
    @Test
    public void testImportWithWrongTypes() {

        // import from a JSON file stored locally on disk, with incorrect type for collection

        // check that entities were created with collection's type

    }


    /**
     * Test that importing bad JSON will result in an informative error message.
     */
    @Test
    public void testImportBadJson() {

        // import from a bad JSON file

        // check that error message indicates JSON parsing error
    }


    /**
     * Simple import test but with multiple files.
     */
    @Test
    public void testImportWithMultipleFiles() {

        // create collection of things in first app, export them to S3

        // create collection of things in second app, export them to S3
    }


    /**
     * Call importService to import files from the configured S3 bucket.
     * @param collectionName Name of collection into which Entities will be imported.
     */
    private void importCollection(final EntityManager em, final String collectionName ) throws Exception {

        logger.debug("\n\nImport into new app {}\n", em.getApplication().getName() );

        ImportService importService = setup.getImportService();
        UUID importUUID = importService.schedule( new HashMap<String, Object>() {{
            put( "path", organization.getName() + em.getApplication().getName());
            put( "organizationId",  organization.getUuid());
            put( "applicationId", em.getApplication().getUuid() );
            put( "collectionName", collectionName);
            put( "properties", new HashMap<String, Object>() {{
                put( "storage_provider", "s3" );
                put( "storage_info", new HashMap<String, Object>() {{
                    put( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR,
                        System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ) );
                    put( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR,
                        System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ) );
                    put( "bucket_location", System.getProperty( "bucketName" ) );
                }});
            }});
        }});

        //  listener.start();

        // TODO countdown latch here?
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
            Thread.sleep(100);
        }

        em.refreshIndex();
    }

    /**
     * Call exportService to export the named collection to the configured S3 bucket.
     */
    private void exportCollection(
        final EntityManager em, final String collectionName ) throws Exception {

        logger.debug("\n\nExporting {} collection from application {}\n",
            collectionName, em.getApplication().getName() );

        em.refreshIndex();

        ExportService exportService = setup.getExportService();
        UUID exportUUID = exportService.schedule( new HashMap<String, Object>() {{
            put( "path", organization.getName() + em.getApplication().getName());
            put( "organizationId",  organization.getUuid());
            put( "applicationId", em.getApplication().getUuid() );
            put( "collectionName", collectionName);
            put( "properties", new HashMap<String, Object>() {{
                 put( "storage_provider", "s3" );
                 put( "storage_info", new HashMap<String, Object>() {{
                     put( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR,
                         System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ) );
                     put( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR,
                         System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ) );
                    put( "bucket_location", System.getProperty( "bucketName" ) );
                }});
            }});
        }});

        // TODO countdown latch here?
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
            Thread.sleep(100);
        }
    }

    /**
     * Create test entities of a specified type.
     * First two entities are connected.
     */
    private void createTestEntities( final EntityManager em,
            Map<UUID, Entity> thingsMap, List<Entity> things, final String type) throws Exception {

        logger.debug("\n\nCreating new {} collection in application {}\n",
            type, em.getApplication().getName() );

        for ( int i = 0; i < 10; i++ ) {
            final int count = i;
            Entity e = em.create( type, new HashMap<String, Object>() {{
                put("name", em.getApplication().getName() + "-" + type + "-" + count);
                put("originalAppId", em.getApplication().getUuid());
                put("originalAppName", em.getApplication().getName());
            }});
            thingsMap.put(e.getUuid(), e);
            things.add( e );
        }

        // first two things are related to each other
        em.createConnection( new SimpleEntityRef( "thing", things.get(0).getUuid()),
            "related",       new SimpleEntityRef( "thing", things.get(1).getUuid()));
        em.createConnection( new SimpleEntityRef( "thing", things.get(1).getUuid()),
            "related",       new SimpleEntityRef( "thing", things.get(0).getUuid()));

        em.refreshIndex();
    }

    @After
    public void after() throws Exception {
        if(listener != null) {
            listener.stop();
            listener = null;
        }
    }

    /**
     * Delete the configured s3 bucket.
     */
    public void deleteBucket() {

        logger.debug("\n\nDelete bucket\n");

        String bucketName = System.getProperty( "bucketName" );
        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        final Iterable<? extends Module> MODULES = ImmutableSet
            .of(new JavaUrlHttpCommandExecutorServiceModule(),
                new Log4JLoggingModule(),
                new NettyPayloadModule());

        BlobStoreContext context =
            ContextBuilder.newBuilder("s3").credentials( accessId, secretKey ).modules( MODULES )
                .overrides( overrides ).buildView( BlobStoreContext.class );

        BlobStore blobStore = context.getBlobStore();
        blobStore.deleteContainer( bucketName );
    }

}

