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

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.management.ManagementService;
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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
@Concurrent
public class ImportServiceIT {

    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceIT.class);

    private static CassandraResource cassandraResource = ServiceITSuite.cassandraResource;
    private static ManagementService managementService;

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
        LOG.info( "in setup" );
        adminUser = setup.getMgmtSvc().createAdminUser( "intern", "intern test", "intern@test.com", "test", false, false );
        organization = setup.getMgmtSvc().createOrganization( "test-organization", adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "test-app" ).getId();

        // add collection with 5 entities
        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        //intialize user object to be posted
        Map<String, Object> userProperties = null;
        Entity[] entity;
        entity = new Entity[5];
        //creates entities
        for ( int i = 0; i < 5; i++ ) {
            userProperties = new LinkedHashMap<String, Object>();
            userProperties.put( "username", "user" + i );
            userProperties.put( "email", "user" + i + "@test.com" );
            entity[i] = em.create( "users", userProperties );
        }

        createTestConnections(em,entity);
    }

    static void createTestConnections(EntityManager em, Entity[] entity) throws Exception {
        //creates connections
        ConnectionRef ref = em.createConnection(em.getRef(entity[0].getUuid()), "related", em.getRef(entity[1].getUuid()));
        em.createConnection( em.getRef( entity[1].getUuid() ), "related", em.getRef( entity[0].getUuid() ) );
    }


    // @Ignore //For this test please input your s3 credentials into settings.xml or Attach a -D with relevant fields.
    @Test
    public void testIntegrationImportCollection() throws Exception {


        ExportService exportService = setup.getExportService();
        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();


        payload.put( "organizationId",  organization.getUuid());
        payload.put( "applicationId", applicationId );
        payload.put("collectionName", "users");

        // export the collection
        UUID exportUUID = exportService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobExportDataCreator(payload, exportUUID, s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
            ;
        }
        //TODo: can check if file got created

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
            ;
        }

        //checks if temp import files are created i.e. downloaded from S3
        assertThat(importService.getEphemeralFile().size(), is(not(0)));

        //check if entities are actually updated i.e. created and modified should be different
        EntityManager em = setup.getEmf().getEntityManager(applicationId);
        EntityRef appRef = em.getApplicationRef();
        Results collections  = em.getCollection(applicationId,"users",null, Results.Level.ALL_PROPERTIES);
        List<Entity> entities = collections.getEntities();
        for(Entity entity: entities) {
            Long created = entity.getCreated();
            Long modified = entity.getModified();
            assertNotEquals(created, modified);
        }

        // check if connections are created
        Results r;
        List<ConnectionRef> connections;

        r = em.getConnectedEntities(entities.get(0).getUuid(), "related", null, Results.Level.IDS );
        connections = r.getConnections();
        assertNotNull( connections );

        r = em.getConnectedEntities(entities.get(1).getUuid(), "related", null, Results.Level.IDS );
        connections = r.getConnections();
        assertNotNull( connections );

        //check if dictionary is created
        EntityRef er;
        Map<Object,Object> dictionaries;

        //check for entity 0
        er = em.getRef(entities.get(0).getUuid());
        dictionaries = em.getDictionaryAsMap(er,"connected_types");
        assertThat(dictionaries.size(),is(not(0)));

        dictionaries = em.getDictionaryAsMap(er,"connecting_types");
        assertThat(dictionaries.size(),is(not(0)));

        //check for entity 1
        er = em.getRef(entities.get(1).getUuid());
        dictionaries = em.getDictionaryAsMap(er,"connected_types");
        assertThat(dictionaries.size(),is(not(0)));

        dictionaries = em.getDictionaryAsMap(er,"connecting_types");
        assertThat(dictionaries.size(),is(not(0)));

        //for entity 2, these should be empty
        er = em.getRef(entities.get(2).getUuid());
        dictionaries = em.getDictionaryAsMap(er,"connected_types");
        assertThat(dictionaries.size(),is(0));

        dictionaries = em.getDictionaryAsMap(er,"connecting_types");
        assertThat(dictionaries.size(),is(0));


    }

    @Test
    public void testIntegrationImportApplication() throws Exception {

        ExportService exportService = setup.getExportService();
        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();


        payload.put( "organizationId",  organization.getUuid());
        payload.put( "applicationId", applicationId );

        // export the collection
        UUID exportUUID = exportService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobExportDataCreator(payload, exportUUID, s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
            ;
        }
        //TODo: can check if file got created

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
            ;
        }
        assertThat(importService.getEphemeralFile().size(), is(not(0)));
    }


    @Test
    public void testIntegrationImportOrganization() throws Exception {


        ExportService exportService = setup.getExportService();
        S3Export s3Export = new S3ExportImpl();
        HashMap<String, Object> payload = payloadBuilder();

        payload.put( "organizationId",  organization.getUuid());

        // export the collection
        UUID exportUUID = exportService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        JobData jobData = jobExportDataCreator(payload, exportUUID, s3Export);

        JobExecution jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        exportService.doExport( jobExecution );
        while ( !exportService.getState( exportUUID ).equals( "FINISHED" ) ) {
            ;
        }
        //TODo: can check if file got created

        // import
        S3Import s3Import = new S3ImportImpl();
        ImportService importService = setup.getImportService();

        UUID importUUID = importService.schedule( payload );

        //create and initialize jobData returned in JobExecution.
        jobData = jobImportDataCreator( payload,importUUID, s3Import );

        jobExecution = mock( JobExecution.class );
        when( jobExecution.getJobData() ).thenReturn( jobData );

        importService.doImport(jobExecution);
        while ( !importService.getState( importUUID ).equals( "FINISHED" ) ) {
            ;
        }
        assertThat(importService.getEphemeralFile().size(), is(not(0)));
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

    public JobData jobImportDataCreator(HashMap<String, Object> payload,UUID importUUID,S3Import s3Import) {
        JobData jobData = new JobData();

        jobData.setProperty( "jobName", "importJob" );
        jobData.setProperty( "importInfo", payload );
        jobData.setProperty( "importId", importUUID );
        jobData.setProperty( "s3Import", s3Import );

        return jobData;
    }

    public JobData jobExportDataCreator(HashMap<String, Object> payload,UUID exportUUID,S3Export s3Export) {
        JobData jobData = new JobData();

        jobData.setProperty( "jobName", "exportJob" );
        jobData.setProperty( "exportInfo", payload );
        jobData.setProperty( "exportId", exportUUID );
        jobData.setProperty( "s3Export", s3Export);

        return jobData;
    }
}
