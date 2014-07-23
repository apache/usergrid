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

package org.apache.usergrid.management.cassandra;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.management.export.S3Export;
import org.apache.usergrid.management.export.S3ExportImpl;
import org.apache.usergrid.management.importUG.ImportService;
import org.apache.usergrid.management.importUG.S3Import;
import org.apache.usergrid.management.importUG.S3ImportImpl;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.JobData;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Ignore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
@Concurrent
public class ImportServiceIT {

    private static final Logger LOG = LoggerFactory.getLogger(ImportServiceIT.class);

    private static CassandraResource cassandraResource = ServiceITSuite.cassandraResource;

    // app-level data generated only once
    private static UserInfo adminUser;
    private static OrganizationInfo organization;
    private static UUID applicationId;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static final ServiceITSetup setup = new ServiceITSetupImpl( cassandraResource );


    @BeforeClass
    public static void setup() throws Exception {
        //creates sample test application
        LOG.info( "in setup" );
        adminUser = setup.getMgmtSvc().createAdminUser( "test", "test user", "test@test.com", "test", false, false );
        organization = setup.getMgmtSvc().createOrganization( "test-organization", adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "test-app" ).getId();
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
            entityTest[i] = emTest.create( "usertests", userProperties );
        }

        //create connection
        emTest.createConnection( emTest.getRef(entityTest[0].getUuid()), "related", emTest.getRef(entityTest[1].getUuid()));
        emTest.createConnection( emTest.getRef(entityTest[1].getUuid()), "related", emTest.getRef( entityTest[0].getUuid()));
    }

    @Ignore //For this test please input your s3 credentials into settings.xml or Attach a -D with relevant fields.
    // test case to check if a collection file is imported correctly
    //@Test
    public void testIntegrationImportCollection() throws Exception {

        // //creates 5 entities in user collection
        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        //intialize user object to be posted
        Map<String, Object> userProperties = null;

        Entity entity[] = new Entity[5];
        //creates entities
        for ( int i = 0; i < 5; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "user" + i );
            userProperties.put( "email", "user" + i + "@test.com" );
            entity[i] = em.create( "users", userProperties );
        }

        //creates test connections between first 2 users
        em.createConnection( em.getRef(entity[0].getUuid()), "related", em.getRef(entity[1].getUuid()));
        em.createConnection( em.getRef(entity[1].getUuid()), "related", em.getRef( entity[0].getUuid()));

        //Export the collection which needs to be tested for import
        ExportService exportService = setup.getExportService();
        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId",  organization.getUuid());
        payload.put( "applicationId", applicationId );
        payload.put("collectionName", "users");

        // schdeule the export job
        UUID exportUUID = exportService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobExportDataCreator(payload, exportUUID, s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //export the collection and wait till export finishes
        exportService.doExport( jobExecution );
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
            ;
        }
        //TODo: can check if temp file got created

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        //schedule the import job
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the collection and wait till import job finishes
        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
            ;
        }

        //checks if temp import files are created i.e. downloaded from S3
        assertThat(importService.getEphemeralFile().size(), is(not(0)));

        //check if entities are actually updated i.e. created and modified should be different
        //EntityManager em = setup.getEmf().getEntityManager(applicationId);
        Results collections  = em.getCollection(applicationId,"users",null, Results.Level.ALL_PROPERTIES);
        List<Entity> entities = collections.getEntities();
        for(Entity eachEntity: entities) {
            Long created = eachEntity.getCreated();
            Long modified = eachEntity.getModified();
            assertThat(created, not(equalTo(modified)));
        }

        // check if connections are created for only the 1st 2 entities in user collection
        Results r;
        List<ConnectionRef> connections;
        for(int i=0;i<2;i++) {
            r = em.getConnectedEntities(entities.get(i).getUuid(), "related", null, Results.Level.IDS);
            connections = r.getConnections();
            assertNotNull(connections);
        }

        //check if dictionary is created
        EntityRef er;
        Map<Object,Object> dictionaries1,dictionaries2;

        for(int i=0;i<3;i++) {

            er = em.getRef(entities.get(i).getUuid());
            dictionaries1 = em.getDictionaryAsMap(er,"connected_types");
            dictionaries2 = em.getDictionaryAsMap(er,"connecting_types");

            if(i==2) {
                //for entity 2, these should be empty
                assertThat(dictionaries1.size(),is(0));
                assertThat(dictionaries2.size(),is(0));
            }
            else {
                assertThat(dictionaries1.size(), is(not(0)));
                assertThat(dictionaries2.size(), is(not(0)));
            }
        }

        //delete bucket
        deleteBucket();
    }

    @Ignore //For this test please input your s3 credentials into settings.xml or Attach a -D with relevant fields.
    // test case to check if application is imported correctly
    //@Test
    public void testIntegrationImportApplication() throws Exception {

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        //intialize user object to be posted
        Map<String, Object> userProperties = null;
//
        //em.createApplicationCollection("custom");
        Entity entity[] = new Entity[5];
        //creates entities for a custom collection called "custom"
        for ( int i = 0; i < 5; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "parameter1", "user" + i );
            userProperties.put( "parameter2", "user" + i + "@test.com" );
            entity[i] = em.create( "customs", userProperties );
        }
        //creates connections
        em.createConnection( em.getRef( entity[0].getUuid() ), "related", em.getRef( entity[1].getUuid() ) );
        em.createConnection( em.getRef( entity[1].getUuid() ), "related", em.getRef( entity[0].getUuid() ) );


        //Export the application which needs to be tested for import
        ExportService exportService = setup.getExportService();
        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId",  organization.getUuid());
        payload.put( "applicationId", applicationId );

        // schedule the export job
        UUID exportUUID = exportService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobExportDataCreator(payload, exportUUID, s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //export the application and wait for the export job to finish
        exportService.doExport( jobExecution );
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
            ;
        }

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        // scheduele the import job
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        // import the application file and wait for it to finish
        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
            ;
        }

        //checks if temp import files are created i.e. downloaded from S3
        assertThat(importService.getEphemeralFile().size(), is(not(0)));

        Set<String> collections = em.getApplicationCollections();
        Iterator<String> collectionsItr = collections.iterator();
        // check if all collections in the application are updated
        while(collectionsItr.hasNext())
        {
            String collectionName = collectionsItr.next();
            Results collection  = em.getCollection(applicationId,collectionName,null, Results.Level.ALL_PROPERTIES);
            List<Entity> entities = collection.getEntities();
            for(Entity eachEntity: entities) {
                Long created = eachEntity.getCreated();
                Long modified = eachEntity.getModified();
                assertThat(created, not(equalTo(modified)));

                //check for dictionaries --> checking permissions in the dictionaries
                EntityRef er;
                Map<Object,Object> dictionaries;

                //checking for permissions for the roles collection
                if(collectionName.equals("roles")) {
                    if(eachEntity.getName().equalsIgnoreCase("admin"))
                    {
                        er = em.getRef(eachEntity.getUuid());
                        dictionaries = em.getDictionaryAsMap(er, "permissions");
                        assertThat(dictionaries.size(), is(0));
                    }
                    else{
                        er = em.getRef(eachEntity.getUuid());
                        dictionaries = em.getDictionaryAsMap(er, "permissions");
                        assertThat(dictionaries.size(), is(not(0)));
                    }
                }
            }
            if(collectionName.equals("customs")) {
                // check if connections are created for only the 1st 2 entities in the custom collection
                Results r;
                List<ConnectionRef> connections;
                for(int i=0;i<2;i++) {
                    r = em.getConnectedEntities(entities.get(i).getUuid(), "related", null, Results.Level.IDS);
                    connections = r.getConnections();
                    assertNotNull(connections);
                }
            }
        }

        //delete bucket
        deleteBucket();
    }

    @Ignore //For this test please input your s3 credentials into settings.xml or Attach a -D with relevant fields.
    // test case to check if all applications file for an organization are imported correctly
    //@Test
    public void testIntegrationImportOrganization() throws Exception {

        // //creates 5 entities in usertests collection
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
        ConnectedEntityRef ref=em.createConnection( em.getRef(entity[0].getUuid()), "related", em.getRef(entity[1].getUuid()));
        em.createConnection( em.getRef(entity[1].getUuid()), "related", em.getRef( entity[0].getUuid()));

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
        //TODo: can check if the temp files got created

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        //schedule the import job
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
            ;
        }

        //checks if temp import files are created i.e. downloaded from S3
        assertThat(importService.getEphemeralFile().size(), is(not(0)));

        //get all applications for an organization
        BiMap<UUID,String> applications = setup.getMgmtSvc().getApplicationsForOrganization(organization.getUuid());
        for (BiMap.Entry<UUID, String> app : applications.entrySet())
        {
            //check if all collections-entities are updated - created and modified should be different
            UUID appID = app.getKey();
            em = setup.getEmf().getEntityManager(appID);
            Set<String> collections = em.getApplicationCollections();
            Iterator<String> itr = collections.iterator();
            while(itr.hasNext())
            {
                String collectionName = itr.next();
                Results collection  = em.getCollection(appID,collectionName,null, Results.Level.ALL_PROPERTIES);
                List<Entity> entities = collection.getEntities();

                for(Entity eachEntity: entities) {
                    Long created = eachEntity.getCreated();
                    Long modified = eachEntity.getModified();
                    assertThat(created, not(equalTo(modified)));
                }


                if(collectionName.equals("usertests")) {
                    
                    // check if connections are created for only the 1st 2 entities in user collection
                    Results r;
                    List<ConnectionRef> connections;
                    for(int i=0;i<2;i++) {
                        r = em.getConnectedEntities(entities.get(i).getUuid(), "related", null, Results.Level.IDS);
                        connections = r.getConnections();
                        assertNotNull(connections);
                    }

                    //check if dictionary is created
                    EntityRef er;
                    Map<Object,Object> dictionaries1,dictionaries2;
                    for(int i=0;i<3;i++) {
                        er = em.getRef(entities.get(i).getUuid());
                        dictionaries1 = em.getDictionaryAsMap(er,"connected_types");
                        dictionaries2 = em.getDictionaryAsMap(er,"connecting_types");

                        if(i==2) {
                            //for entity 2, these should be empty
                            assertThat(dictionaries1.size(),is(0));
                            assertThat(dictionaries2.size(),is(0));
                        }
                        else {
                            assertThat(dictionaries1.size(), is(not(0)));
                            assertThat(dictionaries2.size(), is(not(0)));
                        }
                    }
                }
            }
        }

        //delete bucket
        deleteBucket();
    }

    /**
     * Test to schedule a job with null config
     */
    @Test
    public void testScheduleJobWithNullConfig() throws Exception {
        HashMap<String, Object> payload = null;

        ImportService importService = setup.getImportService();
        UUID importUUID = importService.schedule(payload);

        assertNull(importUUID);
    }

    /**
     * Test to get state of a job with null UUID
     */
    @Test
    public void testGetStateWithNullUUID() throws Exception {
        UUID uuid= null;

        ImportService importService = setup.getImportService();
        String state = importService.getState(uuid);

        assertEquals(state,"UUID passed in cannot be null");
    }

    /**
     * Test to get state of a job with fake UUID
     */
    @Test
    public void testGetStateWithFakeUUID() throws Exception {
        UUID fake = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );

        ImportService importService = setup.getImportService();
        String state = importService.getState(fake);

        assertEquals(state,"No Such Element found");
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
    @Ignore
    //@Test
    public void testDoImportWithNullOrganizationID() throws Exception {
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        //schedule the import job
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        importService.doImport(jobExecution);
        assertEquals(importService.getState(importUUID),"FAILED");
    }

    @Ignore
    /**
     * Test to the doImport method with fake organization ID
     */
    //@Test
    public void testDoImportWithFakeOrganizationID() throws Exception {

        UUID fakeOrgId = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        payload.put("organizationId",fakeOrgId);
        //schedule the import job
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        assertEquals(importService.getState(importUUID),"FAILED");
    }

    /**
     * Test to the doImport method with fake application ID
     */
    @Ignore
    //@Test
    public void testDoImportWithFakeApplicationID() throws Exception {

        UUID fakeappId = UUID.fromString( "AAAAAAAA-FFFF-FFFF-FFFF-AAAAAAAAAAAA" );
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        payload.put("organizationId",organization.getUuid());
        payload.put("applicationId",fakeappId);

        //schedule the import job
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        assertEquals(importService.getState(importUUID),"FAILED");
    }

    /**
     * Test to the doImport Collection method with fake application ID
     */
    //@Test
    @Ignore
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
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        assertEquals(importService.getState(importUUID),"FAILED");
    }

    /**
     * Test to the doImport Collection method with fake application ID
     */
    @Ignore
    //@Test
    public void testDoImportCollectionWithFakeCollectionName() throws Exception {
        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        HashMap<String, Object> payload = payloadBuilder();

        payload.put("organizationId",organization.getUuid());
        payload.put("applicationId",applicationId);
        payload.put("collectionName","fake-collection");

        //schedule the import job
        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobImportDataCreator(payload, importUUID, s3Import);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        //import the all application files for the organization and wait for the import to finish
        importService.doImport(jobExecution);
        assertEquals(importService.getState(importUUID),"FAILED");
        assertEquals(importService.getErrorMessage(importUUID),"Collection Not Found");
    }

    /*Creates fake payload for testing purposes.*/
    public HashMap<String, Object> payloadBuilder() {
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        storage_info.put( "s3_key", System.getProperty( "secretKey" ) );
        storage_info.put( "s3_access_id", System.getProperty( "accessKey" ) );
        storage_info.put( "bucket_location", System.getProperty( "bucketName" ) );

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

        String bucketName = System.getProperty( "bucketName" );
        String accessId = System.getProperty( "accessKey" );
        String secretKey = System.getProperty( "secretKey" );

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
