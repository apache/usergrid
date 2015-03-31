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

package org.apache.usergrid.management.export;


import java.io.File;
import java.io.FileReader;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Service;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.batch.service.JobSchedulerService;
import org.apache.usergrid.utils.UUIDUtils;
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

import org.apache.usergrid.NewOrgAppAdminRule;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.setup.ConcurrentProcessSingleton;

import com.amazonaws.SDKGlobalConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

import static org.apache.usergrid.TestHelper.newUUIDString;
import static org.apache.usergrid.TestHelper.uniqueApp;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.apache.usergrid.persistence.Schema.PROPERTY_APPLICATION_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 *
 *
 */
public class ExportServiceIT {

    private static final Logger logger = LoggerFactory.getLogger( ExportServiceIT.class );


    @ClassRule
    public static final ServiceITSetup setup = new ServiceITSetupImpl(  );

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @Rule
    public NewOrgAppAdminRule newOrgAppAdminRule = new NewOrgAppAdminRule( setup );

    // app-level data generated only once
    private UserInfo adminUser;
    private OrganizationInfo organization;
    private UUID applicationId;

    private static String bucketPrefix;

    private String bucketName;

    @Before
    public void setup() throws Exception {
        logger.info("in setup");

        // start the scheduler after we're all set up
        try {

            JobSchedulerService jobScheduler = ConcurrentProcessSingleton.getInstance().getSpringResource().getBean(JobSchedulerService.class);
            if (jobScheduler.state() != Service.State.RUNNING) {
                jobScheduler.startAsync();
                jobScheduler.awaitRunning();
            }
        } catch ( Exception e ) {
            logger.warn("Ignoring error starting jobScheduler, already started?", e);
        }

        adminUser = newOrgAppAdminRule.getAdminInfo();
        organization = newOrgAppAdminRule.getOrganizationInfo();
        applicationId = newOrgAppAdminRule.getApplicationInfo().getId();

        setup.getEntityIndex().refresh();
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

        adminUser = newOrgAppAdminRule.getAdminInfo();
        organization = newOrgAppAdminRule.getOrganizationInfo();
        applicationId = newOrgAppAdminRule.getApplicationInfo().getId();

        bucketPrefix = System.getProperty( "bucketName" );
        bucketName = bucketPrefix + RandomStringUtils.randomAlphanumeric(10).toLowerCase();
    }


    //Tests to make sure we can call the job with mock data and it runs.
    @Ignore("Connections won't save when run with maven, but on local builds it will.")
    public void testConnectionsOnCollectionExport() throws Exception {

        File f = null;
        int indexCon = 0;

        try {
            f = new File( "testFileConnections.json" );
        }
        catch ( Exception e ) {
            // consumed because this checks to see if the file exists.
            // If it doesn't then don't do anything and carry on.
        }
        f.deleteOnExit();

        S3Export s3Export = new MockS3ExportImpl("testFileConnections.json" );

        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "users" );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[2];
        //creates entities
        for ( int i = 0; i < 2; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "meatIsGreat" + i );
            userProperties.put( "email", "grey" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );

            entity[i] = em.create( "users", userProperties );
        }
        //creates connections
        em.createConnection( em.get( new SimpleEntityRef( "user", entity[0].getUuid() ) ), "Vibrations",
            em.get( new SimpleEntityRef( "user", entity[1].getUuid() ) ) );
        em.createConnection(
                em.get( new SimpleEntityRef( "user", entity[1].getUuid()) ), "Vibrations",
                em.get( new SimpleEntityRef( "user", entity[0].getUuid()) ) );

        UUID exportUUID = exportService.schedule( payload );

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        HashMap<String,Object> jsonMap = mapper.readValue(new FileReader( f ), typeRef);

        Map collectionsMap = (Map)jsonMap.get("collections");
        List usersList = (List)collectionsMap.get("users");

        int indexApp = 0;
        for ( indexApp = 0; indexApp < usersList.size(); indexApp++ ) {
            Map user = (Map)usersList.get( indexApp );
            Map userProps = (Map)user.get("Metadata");
            String uuid = ( String ) userProps.get( "uuid" );
            if ( entity[0].getUuid().toString().equals( uuid ) ) {
                break;
            }
        }

        assertTrue("Uuid was not found in exported files. ", indexApp < usersList.size());

        Map userMap = (Map)usersList.get( indexApp );
        Map connectionsMap = (Map)userMap.get("connections");
        assertNotNull( connectionsMap );

        List vibrationsList = (List)connectionsMap.get( "Vibrations" );

        assertNotNull( vibrationsList );

        f.deleteOnExit();
    }


    @Test //Connections won't save when run with maven, but on local builds it will.
    public void testConnectionsOnApplicationEndpoint() throws Exception {

        File f = null;

        try {
            f = new File( "testConnectionsOnApplicationEndpoint.json" );
        }
        catch ( Exception e ) {
            // consumed because this checks to see if the file exists.
            // If it doesn't then don't do anything and carry on.
        }

        String fileName = "testConnectionsOnApplicationEndpoint.json";

        S3Export s3Export = new MockS3ExportImpl( "testConnectionsOnApplicationEndpoint.json" );

        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder( appName );

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        // intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[2];

        // creates entities
        for ( int i = 0; i < 2; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "billybob" + i );
            userProperties.put( "email", "test" + i + "@anuff.com" );//String.format( "test%i@anuff.com", i ) );

            entity[i] = em.create( "users", userProperties );
        }
        setup.getEntityIndex().refresh();
        //creates connections
        em.createConnection( em.get( new SimpleEntityRef( "user", entity[0].getUuid() ) ), "Vibrations",
            em.get( new SimpleEntityRef( "user", entity[1].getUuid() ) ) );
        em.createConnection(
                em.get( new SimpleEntityRef( "user", entity[1].getUuid())), "Vibrations",
                em.get( new SimpleEntityRef( "user", entity[0].getUuid())) );

        UUID exportUUID = exportService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobDataCreator(payload,exportUUID,s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        HashMap<String,Object> jsonMap = mapper.readValue(new FileReader( f ), typeRef);

        Map collectionsMap = (Map)jsonMap.get("collections");
        List usersList = (List)collectionsMap.get("users");

        int indexApp = 0;
        for ( indexApp = 0; indexApp < usersList.size(); indexApp++ ) {
            Map user = (Map)usersList.get( indexApp );
            Map userProps = (Map)user.get("Metadata");
            String uuid = ( String ) userProps.get( "uuid" );
            if ( entity[0].getUuid().toString().equals( uuid ) ) {
                break;
            }
        }

        assertTrue("Uuid was not found in exported files. ", indexApp < usersList.size());

        Map userMap = (Map)usersList.get( indexApp );
        Map connectionsMap = (Map)userMap.get("connections");
        assertNotNull( connectionsMap );

        List vibrationsList = (List)connectionsMap.get( "Vibrations" );

        assertNotNull( vibrationsList );

        f.deleteOnExit();
    }

    @Test
    public void testExportOneOrgCollectionEndpoint() throws Exception {

        File f = null;


        try {
            f = new File( "exportOneOrg.json" );
        }
        catch ( Exception e ) {
            //consumed because this checks to see if the file exists.
            // If it doesn't then don't do anything and carry on.
        }

        //create another org to ensure we don't export it
        newOrgAppAdminRule.createOwnerAndOrganization(
            "noExport"+newUUIDString(),
            "junkUserName"+newUUIDString(),
            "junkRealName"+newUUIDString(),
            newUUIDString()+"ugExport@usergrid.com",
            "123456789" );

        S3Export s3Export = new MockS3ExportImpl("exportOneOrg.json");
      //  s3Export.setFilename( "exportOneOrg.json" );
        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "roles" );

        UUID exportUUID = exportService.schedule( payload );

        JobData jobData = jobDataCreator(payload,exportUUID,s3Export);


        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonMap = mapper.readValue(new FileReader( f ), typeRef);

        Map collectionsMap = (Map)jsonMap.get("collections");
        String collectionName = (String)collectionsMap.keySet().iterator().next();
        List collection = (List)collectionsMap.get(collectionName);

        for ( Object o : collection ) {
            Map entityMap = (Map)o;
            Map metadataMap = (Map)entityMap.get("Metadata");
            String entityName = (String)metadataMap.get("name");
            assertFalse( "junkRealName".equals( entityName ) );
        }
        f.deleteOnExit();
    }


    //
    //creation of files doesn't always delete itself
    @Test
    public void testExportOneAppOnCollectionEndpoint() throws Exception {

        final String orgName = uniqueOrg();
        final String appName = uniqueApp();


        File f = null;

        try {
            f = new File( "exportOneApp.json" );
        }
        catch ( Exception e ) {
            // consumed because this checks to see if the file exists.
            // If it doesn't, don't do anything and carry on.
        }
        f.deleteOnExit();


        Entity appInfo = setup.getEmf().createApplicationV2(orgName, appName);
        UUID applicationId = UUIDUtils.tryExtractUUID(
            appInfo.getProperty(PROPERTY_APPLICATION_ID).toString());


        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[1];
        //creates entities
        for ( int i = 0; i < 1; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "junkRealName" );
            userProperties.put( "email", "test" + i + "@anuff.com" );
            entity[i] = em.create( "user", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl("exportOneApp.json");
        //s3Export.setFilename( "exportOneApp.json" );
        ExportService exportService = setup.getExportService();

        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        UUID exportUUID = exportService.schedule( payload );

        JobData jobData = jobDataCreator(payload,exportUUID,s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonMap = mapper.readValue(new FileReader( f ), typeRef);

        Map collectionsMap = (Map)jsonMap.get("collections");
        String collectionName = (String)collectionsMap.keySet().iterator().next();
        List collection = (List)collectionsMap.get(collectionName);

        for ( Object o : collection ) {
            Map entityMap = (Map)o;
            Map metadataMap = (Map)entityMap.get("Metadata");
            String entityName = (String)metadataMap.get("name");
            assertFalse( "junkRealName".equals( entityName ) );
        }
    }


    @Test
    public void testExportOneAppOnApplicationEndpointWQuery() throws Exception {

        File f = null;
        try {
            f = new File( "exportOneAppWQuery.json" );
        }
        catch ( Exception e ) {
            // consumed because this checks to see if the file exists.
            // If it doesn't, don't do anything and carry on.
        }
        f.deleteOnExit();


        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[1];
        //creates entities
        for ( int i = 0; i < 1; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "name", "me" );
            userProperties.put( "username", "junkRealName" );
            userProperties.put( "email", "burp" + i + "@anuff.com" );
            entity[i] = em.create( "users", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl("exportOneAppWQuery.json" );
        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "query", "select * where username = 'junkRealName'" );
        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        UUID exportUUID = exportService.schedule( payload );

        JobData jobData = jobDataCreator(payload,exportUUID,s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

       setup.getEntityIndex().refresh();

        exportService.doExport( jobExecution );

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonMap = mapper.readValue(new FileReader( f ), typeRef);

        Map collectionsMap = (Map)jsonMap.get("collections");
        String collectionName = (String)collectionsMap.keySet().iterator().next();
        List collection = (List)collectionsMap.get( collectionName );

        for ( Object o : collection ) {
            Map entityMap = (Map)o;
            Map metadataMap = (Map)entityMap.get("Metadata");
            String entityName = (String)metadataMap.get("name");
            assertFalse( "junkRealName".equals( entityName ) );
        }
    }


    @Test
    public void testExportOneCollection() throws Exception {

        File f = null;
        int entitiesToCreate = 5;

        try {
            f = new File( "exportOneCollection.json" );
        }
        catch ( Exception e ) {
            // consumed because this checks to see if the file exists.
            // If it doesn't, don't do anything and carry on.
        }

        f.deleteOnExit();

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        // em.createApplicationCollection( "qtsMagics" );
        // intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[entitiesToCreate];
        //creates entities
        for ( int i = 0; i < entitiesToCreate; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "billybob" + i );
            userProperties.put( "email", "test" + i + "@anuff.com" );
            entity[i] = em.create( "qtsMagics", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl("exportOneCollection.json" );
        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "qtsMagics" );

        UUID exportUUID = exportService.schedule( payload );

        JobData jobData = jobDataCreator(payload,exportUUID,s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        setup.getEntityIndex().refresh();

        exportService.doExport( jobExecution );

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        HashMap<String,Object> jsonMap = mapper.readValue(new FileReader( f ), typeRef);

        Map collectionsMap = (Map)jsonMap.get("collections");
        String collectionName = (String)collectionsMap.keySet().iterator().next();
        List collection = (List)collectionsMap.get( collectionName );

        assertEquals(entitiesToCreate, collection.size());
    }


    @Test
    public void testExportOneCollectionWQuery() throws Exception {

        File f = null;
        int entitiesToCreate = 5;

        try {
            f = new File( "exportOneCollectionWQuery.json" );
        }
        catch ( Exception e ) {
            // consumed because this checks to see if the file exists.
            // If it doesn't, don't do anything and carry on.
        }
        f.deleteOnExit();

        EntityManager em = setup.getEmf().getEntityManager( applicationId );
        em.createApplicationCollection( "baconators" );
        setup.getEntityIndex().refresh();

        //initialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[entitiesToCreate];

        // creates entities
        for ( int i = 0; i < entitiesToCreate; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "billybob" + i );
            userProperties.put( "email", "test" + i + "@anuff.com" );
            entity[i] = em.create( "baconators", userProperties );
        }

        S3Export s3Export = new MockS3ExportImpl("exportOneCollectionWQuery.json");
        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "query", "select * where username contains 'billybob0'" );
        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );
        payload.put( "collectionName", "baconators" );

        UUID exportUUID = exportService.schedule( payload );

        JobData jobData = jobDataCreator( payload, exportUUID, s3Export );

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        setup.getEntityIndex().refresh();

        exportService.doExport( jobExecution );

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonMap = mapper.readValue(new FileReader( f ), typeRef);

        Map collectionsMap = (Map)jsonMap.get("collections");
        String collectionName = (String)collectionsMap.keySet().iterator().next();
        List collectionList = (List)collectionsMap.get( collectionName );

        assertEquals(1, collectionList.size());
    }


    @Test
    @Ignore("this is a meaningless test because our export format does not support export of organizations")
    public void testExportOneOrganization() throws Exception {

        // create a bunch of organizations, each with applications and collections of entities

        int maxOrgs = 3;
        int maxApps = 3;
        int maxEntities = 20;

        List<ApplicationInfo> appsMade = new ArrayList<>();
        List<OrganizationInfo> orgsMade = new ArrayList<>();

        for ( int orgIndex = 0; orgIndex < maxOrgs; orgIndex++ ) {


            String orgName = "org_" + RandomStringUtils.randomAlphanumeric(10);
            OrganizationInfo orgMade = setup.getMgmtSvc().createOrganization( orgName, adminUser, true );
            orgsMade.add( orgMade );
            logger.debug("Created org {}", orgName);

            for ( int appIndex = 0; appIndex < maxApps; appIndex++ ) {

                String appName =  "app_" + RandomStringUtils.randomAlphanumeric(10);
                ApplicationInfo appMade = setup.getMgmtSvc().createApplication( orgMade.getUuid(), appName );
                appsMade.add( appMade );
                logger.debug("Created app {}", appName);

                for (int entityIndex = 0; entityIndex < maxEntities; entityIndex++) {

                    EntityManager appEm = setup.getEmf().getEntityManager( appMade.getId() );
                    appEm.create( appName + "_type", new HashMap<String, Object>() {{
                        put("property1", "value1");
                        put("property2", "value2");
                    }});
                }
            }
        }

        // export one of the organizations only, using mock S3 export that writes to local disk

        String exportFileName = "exportOneOrganization.json";
        S3Export s3Export = new MockS3ExportImpl( exportFileName );

        HashMap<String, Object> payload = payloadBuilder(appsMade.get(0).getName());
        payload.put("organizationId", orgsMade.get(0).getUuid() );
        payload.put( "applicationId", appsMade.get(0).getId() );

        ExportService exportService = setup.getExportService();
        UUID exportUUID = exportService.schedule( payload );

        JobData jobData = jobDataCreator( payload, exportUUID, s3Export );
        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn(jobData);

        exportService.doExport( jobExecution );

        // finally, we check that file was created and contains the right number of entities in the array

        File exportedFile = new File( exportFileName );
        exportedFile.deleteOnExit();

        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};

        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> jsonMap = mapper.readValue(new FileReader( exportedFile ), typeRef);
        Map collectionsMap = (Map)jsonMap.get("collections");

        List collectionList = (List)collectionsMap.get("users");

        assertEquals( 3, collectionList.size() );
    }


    @Test
    public void testExportDoJob() throws Exception {

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );


        JobData jobData = new JobData();
        jobData.setProperty( "jobName", "exportJob" );

        // this needs to be populated with properties of export info
        jobData.setProperty( "exportInfo", payload );

        JobExecution jobExecution = mock( JobExecution.class );

        when( jobExecution.getJobData() ).thenReturn( jobData );
        when( jobExecution.getJobId() ).thenReturn( UUID.randomUUID() );

        ExportJob job = new ExportJob();
        ExportService eS = mock( ExportService.class );
        job.setExportService( eS );
        try {
            job.doJob( jobExecution );
        }
        catch ( Exception e ) {
            logger.error("Error doing job", e);
            assert ( false );
        }
        assert ( true );
    }

    //tests that with empty job data, the export still runs.
    @Test
    public void testExportEmptyJobData() throws Exception {

        JobData jobData = new JobData();

        JobExecution jobExecution = mock( JobExecution.class );

        when( jobExecution.getJobData() ).thenReturn( jobData );
        when( jobExecution.getJobId() ).thenReturn( UUID.randomUUID() );

        ExportJob job = new ExportJob();
        S3Export s3Export = mock( S3Export.class );
        //setup.getExportService().setS3Export( s3Export );
        job.setExportService( setup.getExportService() );
        try {
            job.doJob( jobExecution );
        }
        catch ( Exception e ) {
            assert ( false );
        }
        assert ( true );
    }


    @Test
    public void testNullJobExecution() {

        JobData jobData = new JobData();

        JobExecution jobExecution = mock( JobExecution.class );

        when( jobExecution.getJobData() ).thenReturn( jobData );
        when( jobExecution.getJobId() ).thenReturn( UUID.randomUUID() );

        ExportJob job = new ExportJob();
        S3Export s3Export = mock( S3Export.class );
       // setup.getExportService().setS3Export( s3Export );
        job.setExportService( setup.getExportService() );
        try {
            job.doJob( jobExecution );
        }
        catch ( Exception e ) {
            assert ( false );
        }
        assert ( true );
    }


    @Test
    @Ignore // TODO: fix this test...
    public void testIntegration100EntitiesOn() throws Exception {

        logger.debug("testIntegration100EntitiesOn(): starting...");

        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        // create five applications each with collection of five entities

        for ( int i = 0; i < 5; i++ ) {

            ApplicationInfo appMade = setup.getMgmtSvc().createApplication( organization.getUuid(), "superapp" + i );
            EntityManager appEm = setup.getEmf().getEntityManager( appMade.getId() );

            String collName = "superappCol" + i;
            appEm.createApplicationCollection(collName);

            Map<String, Object> entityLevelProperties = null;
            Entity[] entNotCopied;
            entNotCopied = new Entity[5];

            for ( int index = 0; index < 5; index++ ) {
                entityLevelProperties = new LinkedHashMap<String, Object>();
                entityLevelProperties.put( "username", "bobso" + index );
                entityLevelProperties.put( "email", "derp" + index + "@anuff.com" );
                entNotCopied[index] = appEm.create( collName, entityLevelProperties );
            }
        }

        // export the organization containing those apps and collections

        UUID exportUUID = exportService.schedule( payload );

        int maxRetries = 100;
        int retries = 0;
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) && retries++ < maxRetries ) {
            Thread.sleep(100);
        }

        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );
        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        // test that we can find the file that were exported to S3

        BlobStore blobStore = null;
        try {

            final Iterable<? extends Module> MODULES = ImmutableSet.of(
                new JavaUrlHttpCommandExecutorServiceModule(),
                new Log4JLoggingModule(),
                new NettyPayloadModule());

            BlobStoreContext context = ContextBuilder.newBuilder("s3")
                .credentials(accessId, secretKey)
                .modules(MODULES)
                .overrides(overrides)
                .buildView(BlobStoreContext.class);

            String expectedFileName = ((ExportServiceImpl) exportService)
                .prepareOutputFileName(organization.getName(), "applications");

            blobStore = context.getBlobStore();
            if (!blobStore.blobExists(bucketName, expectedFileName)) {
                blobStore.deleteContainer(bucketName);
                Assert.fail("Blob does not exist: " + expectedFileName);
            }
            Blob bo = blobStore.getBlob(bucketName, expectedFileName);

            Long numOfFiles = blobStore.countBlobs(bucketName);
            Long numWeWant = 1L;
            blobStore.deleteContainer(bucketName);
            assertEquals(numOfFiles, numWeWant);
            assertNotNull(bo);

        } finally {
            blobStore.deleteContainer(bucketName);
        }
    }

    @Ignore("Why is this ignored?")
    @Test
    public void testIntegration100EntitiesForAllApps() throws Exception {

        S3Export s3Export = new S3ExportImpl();
        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        OrganizationInfo orgMade = null;
        ApplicationInfo appMade = null;
        for ( int i = 0; i < 5; i++ ) {
            orgMade = setup.getMgmtSvc().createOrganization( "minorboss" + i, adminUser, true );
            for ( int j = 0; j < 5; j++ ) {
                appMade = setup.getMgmtSvc().createApplication( orgMade.getUuid(), "superapp" + j );

                EntityManager customMaker = setup.getEmf().getEntityManager( appMade.getId() );
                customMaker.createApplicationCollection( "superappCol" + j );
                //intialize user object to be posted
                Map<String, Object> entityLevelProperties = null;
                Entity[] entNotCopied;
                entNotCopied = new Entity[1];
                //creates entities
                for ( int index = 0; index < 1; index++ ) {
                    entityLevelProperties = new LinkedHashMap<String, Object>();
                    entityLevelProperties.put( "derp", "bacon" );
                    entNotCopied[index] = customMaker.create( "superappCol" + j, entityLevelProperties );
                }
            }
        }

        payload.put( "organizationId", orgMade.getUuid() );

        UUID exportUUID = exportService.schedule( payload );
        assertNotNull( exportUUID );

        Thread.sleep( 3000 );

        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        BlobStore blobStore = null;

        try {
            final Iterable<? extends Module> MODULES = ImmutableSet.of(
                new JavaUrlHttpCommandExecutorServiceModule(),
                new Log4JLoggingModule(),
                new NettyPayloadModule() );

            BlobStoreContext context = ContextBuilder.newBuilder( "s3" )
                .credentials(accessId, secretKey )
                .modules(MODULES )
                .overrides(overrides )
                .buildView(BlobStoreContext.class );

            blobStore = context.getBlobStore();

            //Grab Number of files
            Long numOfFiles = blobStore.countBlobs( bucketName );

            String expectedFileName = ((ExportServiceImpl)exportService)
                .prepareOutputFileName(organization.getName(), "applications");

            //delete container containing said files
            Blob bo = blobStore.getBlob(bucketName, expectedFileName);
            Long numWeWant = 5L;
            blobStore.deleteContainer( bucketName );

            //asserts that the correct number of files was transferred over
            assertEquals( numWeWant, numOfFiles );

        }
        finally {
            blobStore.deleteContainer( bucketName );
        }
    }


    @Ignore("Why is this ignored")
    @Test
    public void testIntegration100EntitiesOnOneOrg() throws Exception {

        S3Export s3Export = new S3ExportImpl();
        ExportService exportService = setup.getExportService();

        String appName = newOrgAppAdminRule.getApplicationInfo().getName();
        HashMap<String, Object> payload = payloadBuilder(appName);

        payload.put( "organizationId", organization.getUuid() );
        payload.put( "applicationId", applicationId );

        OrganizationInfo orgMade = null;
        ApplicationInfo appMade = null;
        for ( int i = 0; i < 100; i++ ) {
            orgMade = setup.getMgmtSvc().createOrganization( "largerboss" + i, adminUser, true );
            appMade = setup.getMgmtSvc().createApplication( orgMade.getUuid(), "superapp" + i );

            EntityManager customMaker = setup.getEmf().getEntityManager( appMade.getId() );
            customMaker.createApplicationCollection( "superappCol" + i );
            //intialize user object to be posted
            Map<String, Object> entityLevelProperties = null;
            Entity[] entNotCopied;
            entNotCopied = new Entity[20];
            //creates entities
            for ( int index = 0; index < 20; index++ ) {
                entityLevelProperties = new LinkedHashMap<String, Object>();
                entityLevelProperties.put( "username", "bobso" + index );
                entityLevelProperties.put( "email", "derp" + index + "@anuff.com" );
                entNotCopied[index] = customMaker.create( "superappCol", entityLevelProperties );
            }
        }

        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[100];

        //creates entities
        for ( int i = 0; i < 100; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "bido" + i );
            userProperties.put( "email", "bido" + i + "@anuff.com" );

            entity[i] = em.create( "user", userProperties );
        }

        UUID exportUUID = exportService.schedule( payload );

        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {}

        String accessId = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        String secretKey = System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );

        Properties overrides = new Properties();
        overrides.setProperty( "s3" + ".identity", accessId );
        overrides.setProperty( "s3" + ".credential", secretKey );

        Blob bo = null;
        BlobStore blobStore = null;

        try {
            final Iterable<? extends Module> MODULES = ImmutableSet.of( new JavaUrlHttpCommandExecutorServiceModule(),
                new Log4JLoggingModule(), new NettyPayloadModule() );

            BlobStoreContext context = ContextBuilder.newBuilder( "s3" )
                .credentials( accessId, secretKey )
                .modules( MODULES )
                .overrides( overrides )
                .buildView( BlobStoreContext.class );

            String expectedFileName = ((ExportServiceImpl)exportService)
                .prepareOutputFileName(organization.getName(), "applications");

            blobStore = context.getBlobStore();
            if ( !blobStore.blobExists( bucketName, expectedFileName ) ) {
                assert ( false );
            }
            Long numOfFiles = blobStore.countBlobs( bucketName );
            Long numWeWant = Long.valueOf( 1 );
            assertEquals( numOfFiles, numWeWant );

            bo = blobStore.getBlob( bucketName, expectedFileName );
        }
        catch ( Exception e ) {
            assert ( false );
        }

        assertNotNull( bo );
        blobStore.deleteContainer( bucketName );
    }

    public JobData jobDataCreator(HashMap<String, Object> payload,UUID exportUUID, S3Export s3Export) {
        JobData jobData = new JobData();

        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );
        jobData.setProperty( "s3Export", s3Export );

        return jobData;
    }

    /*Creates fake payload for testing purposes.*/
    public HashMap<String, Object> payloadBuilder( String orgOrAppName ) {
        HashMap<String, Object> payload = new HashMap<String, Object>();
        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, Object> storage_info = new HashMap<String, Object>();
        storage_info.put( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR,
            System.getProperty( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR ) );
        storage_info.put( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR,
            System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR ) );
        storage_info.put( "bucket_location",  bucketName );

        properties.put( "storage_provider", "s3" );
        properties.put( "storage_info", storage_info );

        payload.put( "path", orgOrAppName );
        payload.put( "properties", properties );
        return payload;
    }
}
