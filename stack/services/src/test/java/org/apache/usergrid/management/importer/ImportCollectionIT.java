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
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Module;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.management.export.S3Export;
import org.apache.usergrid.management.export.S3ExportImpl;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.services.notifications.QueueListener;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


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

        final EntityManager emApp1 = setup.getEmf().getEntityManager( applicationId );

        // create a collection of "thing" entities in the default test application

        Map<UUID, Entity> thingsMap = new HashMap<>();
        List<Entity> things = new ArrayList<>();

        createTestEntities(emApp1, thingsMap, things);

        // export the "things" collection to a JSON file in an S3 bucket

        exportCollection(emApp1);

        // create new application

        final UUID appId2 = setup.getMgmtSvc().createApplication(
            organization.getUuid(), "noobapp" ).getId();

        // import the data into the new application

        final EntityManager emApp2 = setup.getEmf().getEntityManager(appId2);
        importCollection(appId2, emApp2);

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

            logger.debug("\n\nCheck dictionary\n");

            // check if dictionary is created
            EntityRef er;
            Map<Object, Object> dictionaries1, dictionaries2;

            for (int i = 0; i < 3; i++) {

                er = importedThings.get(i);
                dictionaries1 = emApp2.getDictionaryAsMap(er, "connected_types");
                dictionaries2 = emApp2.getDictionaryAsMap(er, "connecting_types");

                if (i == 2) {
                    //for entity 2, these should be empty
                    assertThat(dictionaries1.size(), is(0));
                    assertThat(dictionaries2.size(), is(0));
                } else {
                    assertThat(dictionaries1.size(), is(not(0)));
                    assertThat(dictionaries2.size(), is(not(0)));
                }
            }

            // if entities are deleted from app1, they still exist in app2
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

    private void importCollection(final UUID appId2, final EntityManager em2) throws Exception {

        logger.debug("\n\nImport into new app {}\n", appId2 );

        ImportService importService = setup.getImportService();
        UUID importUUID = importService.schedule( new HashMap<String, Object>() {{
            put( "path", organization.getName() + em2.getApplication().getName());
            put( "organizationId",  organization.getUuid());
            put( "applicationId", appId2 );
            put( "collectionName", "things");
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

        em2.refreshIndex();
    }

    private void exportCollection(final EntityManager em) throws Exception {

        logger.debug("\n\nExport\n");

        ExportService exportService = setup.getExportService();
        UUID exportUUID = exportService.schedule( new HashMap<String, Object>() {{
            put( "path", organization.getName() + em.getApplication().getName());
            put( "organizationId",  organization.getUuid());
            put( "applicationId", applicationId );
            put( "collectionName", "things");
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

    private void createTestEntities(
            EntityManager em, Map<UUID, Entity> thingsMap, List<Entity> things) throws Exception {

        logger.debug("\n\nCreate things collection\n");

        for ( int i = 0; i < 10; i++ ) {
            final int count = i;
            Entity e = em.create( "thing", new HashMap<String, Object>() {{
                put("name", "thing" + count);
                put("index", count);
            }});
            thingsMap.put(e.getUuid(), e);
            things.add( e );
        }

        logger.debug("\n\nCreate Connections\n");

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

    // delete the s3 bucket which was created for testing
    public void deleteBucket() {

        logger.debug("\n\nDelete bucket\n");

        String bucketName = System.getProperty( "bucketName" );
        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        Blob bo = null;
        BlobStore blobStore = null;
        final Iterable<? extends Module> MODULES = ImmutableSet
            .of(new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
                new NettyPayloadModule());

        BlobStoreContext context =
            ContextBuilder.newBuilder("s3").credentials( accessId, secretKey ).modules( MODULES )
                .overrides( overrides ).buildView( BlobStoreContext.class );

        blobStore = context.getBlobStore();
        blobStore.deleteContainer( bucketName );
    }

}

