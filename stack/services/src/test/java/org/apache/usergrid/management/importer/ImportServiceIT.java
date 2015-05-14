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
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.management.export.S3Export;
import org.apache.usergrid.management.export.S3ExportImpl;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.Query.Level;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.services.notifications.QueueListener;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;

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

import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


//@Concurrent
public class ImportServiceIT {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceIT.class);

    // app-level data generated only once
    private static UserInfo adminUser;
    private static OrganizationInfo organization;
    private static UUID applicationId;

    QueueListener listener;

    final String bucketName = System.getProperty( "bucketName" )
        + RandomStringUtils.randomAlphanumeric(10).toLowerCase();

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static final ServiceITSetup setup =
        new ServiceITSetupImpl(  );


    @BeforeClass
    public static void setup() throws Exception {
        String username = "test"+ UUIDUtils.newTimeUUID();

        // start the scheduler after we're all set up
         // start the scheduler after we're all set up
        JobSchedulerService jobScheduler = ConcurrentProcessSingleton
            .getInstance().getSpringResource().getBean( JobSchedulerService.class );

        if ( jobScheduler.state() != Service.State.RUNNING ) {
            jobScheduler.startAsync();
            jobScheduler.awaitRunning();
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

    @After
    public void after() throws Exception {
        if(listener != null) {
            listener.stop();
            listener = null;
        }
    }

    // test case to check if application is imported correctly
    @Test
    @Ignore("Import organization not supported")
    public void testImportApplication() throws Exception {

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        // Create five user entities (we already have one admin user)
        List<Entity> entities = new ArrayList<>();
        for ( int i = 0; i < 5; i++ ) {
            Map<String, Object> userProperties =  new LinkedHashMap<>();
            userProperties.put( "parameter1", "user" + i );
            userProperties.put( "parameter2", "user" + i + "@test.com" );
            entities.add( em.create( "custom", userProperties ) );
        }
        // Creates connections
        em.createConnection( new SimpleEntityRef( "custom",  entities.get(0).getUuid() ),
                  "related", new SimpleEntityRef( "custom",  entities.get(1).getUuid() ) );
        em.createConnection( new SimpleEntityRef( "custom",  entities.get(1).getUuid() ),
                  "related", new SimpleEntityRef( "custom",  entities.get(0).getUuid() ) );

        logger.debug("\n\nExport the application\n\n");

        // Export the application which needs to be tested for import
        ExportService exportService = setup.getExportService();
        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();
        payload.put( "organizationId",  organization.getUuid());
        payload.put( "applicationId", applicationId );

        // Schedule the export job
        UUID exportUUID = exportService.schedule( payload );

        // Create and initialize jobData returned in JobExecution.
        JobData jobData = jobExportDataCreator(payload, exportUUID, s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        // Export the application and wait for the export job to finish
        exportService.doExport( jobExecution );
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
           // wait...
        }

        logger.debug("\n\nImport the application\n\n");

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        // scheduele the import job
        final Import importEntity = importService.schedule( null,  payload );
        final UUID importUUID = importEntity.getUuid();

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        // import the application file and wait for it to finish
        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
           // wait...
        }

        logger.debug("\n\nVerify Import\n\n");

        try {
            //checks if temp import files are created i.e. downloaded from S3
            //assertThat(importService.getEphemeralFile().size(), is(not(0)));

            Set<String> collections = em.getApplicationCollections();

            // check if all collections in the application are updated
            for (String collectionName : collections) {
                logger.debug("Checking collection {}", collectionName);

                Results collection = em.getCollection(applicationId, collectionName, null, Level.ALL_PROPERTIES);

                for (Entity eachEntity : collection.getEntities() ) {

                    logger.debug("Checking entity {} {}:{}",
                        new Object[] { eachEntity.getName(), eachEntity.getType(), eachEntity.getUuid()} );

                    //check for dictionaries --> checking permissions in the dictionaries
                    EntityRef er;
                    Map<Object, Object> dictionaries;

                    //checking for permissions for the roles collection
                    if (collectionName.equals("roles")) {
                        if (eachEntity.getName().equalsIgnoreCase("admin")) {
                            er = eachEntity;
                            dictionaries = em.getDictionaryAsMap(er, "permissions");
                            assertThat(dictionaries.size(), is(not(0))); // admin has permission
                        } else {
                            er = eachEntity;
                            dictionaries = em.getDictionaryAsMap(er, "permissions");
                            assertThat(dictionaries.size(), is(0)); // other roles do not
                        }
                    }
                }

                if (collectionName.equals("customs")) {
                    // check if connections are created for only the 1st 2 entities in the custom collection
                    Results r;
                    List<ConnectionRef> connections;
                    for (int i = 0; i < 2; i++) {
                        r = em.getTargetEntities(entities.get(i), "related", null, Level.IDS);
                        connections = r.getConnections();
                        assertNotNull(connections);
                    }
                }
            }
        }
        finally {
            //delete bucket
            deleteBucket();
        }
    }

    // test case to check if all applications file for an organization are imported correctly
    @Test
    @Ignore("Import organization not supported")
    public void testImportOrganization() throws Exception {

        // creates 5 entities in usertests collection
        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        //intialize user object to be posted
        Map<String, Object> userProperties = null;

        Entity entity[] = new Entity[5];
        //creates entities
        for ( int i = 0; i < 5; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "name", "user" + i );
            userProperties.put( "email", "user" + i + "@test.com" );
            entity[i] = em.create( "usertests", userProperties );
            em.getCollections(entity[i]).contains("usertests");
        }

        //creates test connections between first 2 entities in usertests collection
        ConnectedEntityRef ref = em.createConnection( entity[0], "related", entity[1]);

        em.createConnection( entity[1], "related", entity[0]);

        //create 2nd test application, add entities to it, create connections and set permissions
        createAndSetup2ndApplication();

        //export all applications in an organization
        ExportService exportService = setup.getExportService();
        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId",  organization.getUuid());

        //schdeule the export job
        UUID exportUUID = exportService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobExportDataCreator(payload, exportUUID, s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //export organization data and wait for the export job to finish
        exportService.doExport( jobExecution );
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
            ;
        }
        //TODO: can check if the temp files got created

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        //schedule the import job
        final Import importEntity = importService.schedule(  null, payload );
        final UUID importUUID = importEntity.getUuid();

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( Import.State.FINISHED ) ) {
            ;
        }

        try {
            //checks if temp import files are created i.e. downloaded from S3
            //assertThat(importService.getEphemeralFile().size(), is(not(0)));

            //get all applications for an organization
            BiMap<UUID, String> applications =
                setup.getMgmtSvc().getApplicationsForOrganization(organization.getUuid());

            for (BiMap.Entry<UUID, String> app : applications.entrySet()) {

                //check if all collections-entities are updated - created and modified should be different
                UUID appID = app.getKey();
                em = setup.getEmf().getEntityManager(appID);
                Set<String> collections = em.getApplicationCollections();
                Iterator<String> itr = collections.iterator();
                while (itr.hasNext()) {
                    String collectionName = itr.next();
                    Results collection = em.getCollection(appID, collectionName, null, Level.ALL_PROPERTIES);
                    List<Entity> entities = collection.getEntities();

                    if (collectionName.equals("usertests")) {

                        // check if connections are created for only the 1st 2 entities in user collection
                        Results r;
                        List<ConnectionRef> connections;
                        for (int i = 0; i < 2; i++) {
                            r = em.getTargetEntities(entities.get(i), "related", null, Level.IDS);
                            connections = r.getConnections();
                            assertNotNull(connections);
                        }

                        //check if dictionary is created
                        EntityRef er;
                        Map<Object, Object> dictionaries1, dictionaries2;
                        for (int i = 0; i < 3; i++) {
                            er = entities.get(i);
                            dictionaries1 = em.getDictionaryAsMap(er, "connected_types");
                            dictionaries2 = em.getDictionaryAsMap(er, "connecting_types");

                            if (i == 2) {
                                //for entity 2, these should be empty
                                assertThat(dictionaries1.size(), is(0));
                                assertThat(dictionaries2.size(), is(0));
                            } else {
                                assertThat(dictionaries1.size(), is(not(0)));
                                assertThat(dictionaries2.size(), is(not(0)));
                            }
                        }
                    }
                }
            }
        }
        finally {
            //delete bucket
            deleteBucket();
        }
    }

    /**
     * Test to schedule a job with null config
     */
    @Test(expected=NullPointerException.class)
    public void testScheduleJobWithNullConfig() throws Exception {
        HashMap<String, Object> payload = null;

        ImportService importService = setup.getImportService();
        final Import importEntity = importService.schedule( null,  payload );


        assertNull(importEntity);
    }

    /**
     * Test to get state of a job with null UUID
     */
    @Test(expected = NullPointerException.class)
    public void testGetStateWithNullUUID() throws Exception {
        UUID uuid= null;

        ImportService importService = setup.getImportService();
        Import.State state = importService.getState( uuid );

    }

    /**
     * Test to get state of a job with fake UUID
     */
    @Test(expected = EntityNotFoundException.class)
    public void testGetStateWithFakeUUID() throws Exception {
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );

        ImportService importService = setup.getImportService();
        Import.State state = importService.getState( fake );

    }


    /**
     * Test to get error message of a job with null state
     */
    @Test
    public void testErrorMessageWithNullState() throws Exception {
        UUID state = null;
        ImportService importService = setup.getImportService();
        String error = importService.getErrorMessage(state);

        assertEquals(error,"UUID passed in cannot be null");
    }

    /**
     * Test to get error message of a job with fake UUID
     */
    @Test
    public void testErrorMessageWithFakeUUID() throws Exception {
        UUID state = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        ImportService importService = setup.getImportService();
        String error = importService.getErrorMessage(state);

        assertEquals(error,"No Such Element found");
    }

    /**
     * Test to the doImport method with null organziation ID
     */
    @Test
    @Ignore("Import organization not supported")
    public void testDoImportWithNullOrganizationID() throws Exception {
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        //schedule the import job
        final Import importEntity = importService.schedule( null,  payload );
        final UUID importUUID = importEntity.getUuid();

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        importService.doImport(jobExecution);
        assertEquals(importService.getState(importUUID),Import.State.FAILED);
    }

    /**
     * Test to the doImport method with fake organization ID
     */
    @Test
    @Ignore("Import organization not supported")
    public void testDoImportWithFakeOrganizationID() throws Exception {

        UUID fakeOrgId = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        payload.put("organizationId",fakeOrgId);
        //schedule the import job
        final Import importEntity = importService.schedule( null,  payload );
        final UUID importUUID = importEntity.getUuid();

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        assertEquals(Import.State.FAILED, importService.getState(importUUID));
    }

    /**
     * Test to the doImport method with fake application ID
     */
    @Test
    @Ignore("Import application not supported")
    public void testDoImportWithFakeApplicationID() throws Exception {

        UUID fakeappId = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        payload.put("organizationId",organization.getUuid());
        payload.put("applicationId",fakeappId);

        //schedule the import job
        final Import importEntity = importService.schedule(  null, payload );
        final UUID importUUID = importEntity.getUuid();

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        assertEquals(Import.State.FAILED, importService.getState(importUUID));
    }

    /**
     * Test to the doImport Collection method with fake application ID
     */
    @Test
    @Ignore("Import application not supported")
    public void testDoImportCollectionWithFakeApplicationID() throws Exception {

        UUID fakeappId = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        payload.put("organizationId",organization.getUuid());
        payload.put("applicationId",fakeappId);
        payload.put("collectionName","custom-test");

        //schedule the import job
        final Import importEntity = importService.schedule( null,  payload );
        final UUID importUUID = importEntity.getUuid();

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all collection files for the organization-application and wait for the import to finish
        importService.doImport(jobExecution);
        assertEquals(importService.getState(importUUID),Import.State.FAILED);
    }

    /*Creates fake payload for testing purposes.*/
    public HashMap<String, Object> payloadBuilder() {

        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        storage_info.put( "bucket_location", bucketName );

        storage_info.put( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR,
            System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ) );
        storage_info.put( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR,
            System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ) );

        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );

        payload.put( "path","test-organization/test-app" );
        payload.put( "properties", properties );
        return payload;
    }

    //creates fake import job
    public JobData jobImportDataCreator(HashMap<String, Object> payload,UUID importUUID,S3Import s3Import) {
        JobData jobData = new JobData();

        jobData.setProperty( "jobName", "importJob" );
        jobData.setProperty( "importInfo", payload );
        jobData.setProperty( "importId", importUUID );
        jobData.setProperty( "s3Import", s3Import );

        return jobData;
    }

    //creates fake export job
    public JobData jobExportDataCreator(HashMap<String, Object> payload,UUID exportUUID,S3Export s3Export) {
        JobData jobData = new JobData();

        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );
        jobData.setProperty( "s3Export", s3Export);

        return jobData;
    }

    // delete the s3 bucket which was created for testing
    public void deleteBucket() {

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

    //creates 2nd application for testing import from an organization having multiple applications
    void createAndSetup2ndApplication() throws Exception {

        UUID appId = setup.getMgmtSvc().createApplication( organization.getUuid(), "test-app-2" ).getId();
        EntityManager emTest = setup.getEmf().getEntityManager(appId);

        Map<String, Object> userProperties = null;

        Entity entityTest[] = new Entity[5];

        //creates entities and set permissions
        for ( int i = 0; i < 5; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "name", "user" + i );
            userProperties.put( "email", "user" + i + "@test.com" );
            entityTest[i] = emTest.create( "testobject", userProperties );
        }

        //create connection
        emTest.createConnection( new SimpleEntityRef( "testobject",  entityTest[0].getUuid()),
            "related",
            new SimpleEntityRef( "testobject",  entityTest[1].getUuid()));

        emTest.createConnection( new SimpleEntityRef( "testobject",  entityTest[1].getUuid()),
            "related",
            new SimpleEntityRef( "testobject",  entityTest[0].getUuid()));
    }
}
