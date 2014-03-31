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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.service.SchedulerService;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.PagingResultsIterator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.entities.Export;
import org.apache.usergrid.persistence.entities.JobData;

import com.google.common.collect.BiMap;


/**
 * Need to refactor out the mutliple orgs being take , need to factor out the multiple apps it will just be the one app
 * and the one org and all of it's collections.
 */
public class ExportServiceImpl implements ExportService {


    private static final Logger logger = LoggerFactory.getLogger( ExportServiceImpl.class );
    public static final String EXPORT_ID = "exportId";
    public static final String EXPORT_JOB_NAME = "exportJob";
    //dependency injection
    private SchedulerService sch;

    //injected the Entity Manager Factory
    protected EntityManagerFactory emf;

    //inject Management Service to access Organization Data
    private ManagementService managementService;

    //Maximum amount of entities retrieved in a single go.
    public static final int MAX_ENTITY_FETCH = 1000;

    //Amount of time that has passed before sending another heart beat in millis
    public static final int TIMESTAMP_DELTA = 5000;

    private JsonFactory jsonFactory = new JsonFactory();

    private S3Export s3Export;

    private String defaultAppExportname = "exporters";


    @Override
    public UUID schedule( final Map<String, Object> config ) throws Exception {
        ApplicationInfo defaultExportApp = null;

        if ( config == null ) {
            logger.error( "export information cannot be null" );
            return null;
        }

        if ( config.get( "applicationId" ) == null ) {
            defaultExportApp = managementService
                    .createApplication( ( UUID ) config.get( "organizationId" ), defaultAppExportname );
            config.put( "applicationId", defaultExportApp.getId() );
            //logger.error( "application information from export info could not be found" );
            //return null;
        }

        EntityManager em = null;
        try {
            em = emf.getEntityManager( ( UUID ) config.get( "applicationId" ) );
        }
        catch ( Exception e ) {
            logger.error( "application doesn't exist within the current context" );
            return null;
        }

        Export export = new Export();

        //update state
        try {
            export = em.create( export );
        }
        catch ( Exception e ) {
            logger.error( "Export entity creation failed" );
            return null;
        }

        export.setState( Export.State.CREATED );
        em.update( export );

        //set data to be transferred to exportInfo
        JobData jobData = new JobData();
        jobData.setProperty( "exportInfo", config );
        jobData.setProperty( EXPORT_ID, export.getUuid() );

        long soonestPossible = System.currentTimeMillis() + 250; //sch grace period

        //schedule job
        sch.createJob( EXPORT_JOB_NAME, soonestPossible, jobData );

        //update state
        export.setState( Export.State.SCHEDULED );
        em.update( export );

        return export.getUuid();
    }


    /**
     * Query Entity Manager for the string state of the Export Entity. This corresponds to the GET /export
     *
     * @return String
     */
    @Override
    public String getState( final UUID appId, final UUID uuid ) throws Exception {

        //get application entity manager
        if ( appId == null ) {
            logger.error( "Application context cannot be found." );
            return "Application context cannot be found.";
        }

        if ( uuid == null ) {
            logger.error( "UUID passed in cannot be null." );
            return "UUID passed in cannot be null";
        }

        EntityManager rootEm = emf.getEntityManager( appId );

        //retrieve the export entity.
        Export export = rootEm.get( uuid, Export.class );

        if ( export == null ) {
            logger.error( "no entity with that uuid was found" );
            return "No Such Element found";
        }
        return export.getState().toString();
    }


    @Override
    public void doExport( final JobExecution jobExecution ) throws Exception {
        Map<String, Object> config = ( Map<String, Object> ) jobExecution.getJobData().getProperty( "exportInfo" );

        UUID scopedAppId = ( UUID ) config.get( "applicationId" );

        if ( config == null ) {
            logger.error( "Export Information passed through is null" );
            return;
        }
        //get the entity manager for the application, and the entity that this Export corresponds to.
        UUID exportId = ( UUID ) jobExecution.getJobData().getProperty( EXPORT_ID );
        if ( scopedAppId == null ) {
            logger.error( "Export Information application uuid is null" );
            return;
        }
        EntityManager em = emf.getEntityManager( scopedAppId );
        Export export = em.get( exportId, Export.class );

        //update the entity state to show that the job has officially started.
        export.setState( Export.State.STARTED );
        em.update( export );

        if ( em.getApplication().getApplicationName().equals( "exporters" ) ) {
            exportApplicationsFromOrg( ( UUID ) config.get( "organizationId" ), config, jobExecution );
        }
        else if ( config.get( "collectionName" ) == null ) {
            //exports all the applications for a given organization.

            if ( config.get( "organizationId" ) == null ) {
                logger.error( "No organization could be found" );
                export.setState( Export.State.FAILED );
                em.update( export );
                return;
            }
            exportApplicationFromOrg( ( UUID ) config.get( "organizationId" ), ( UUID ) config.get( "applicationId" ),
                    config, jobExecution );
        }
        else {
            try {
                //exports all the applications for a single organization
                if ( config.get( "organizationId" ) == null ) {
                    logger.error( "No organization could be found" );
                    export.setState( Export.State.FAILED );
                    em.update( export );
                    return;
                }
                exportCollectionFromOrgApp( ( UUID ) config.get( "organizationId" ),
                        ( UUID ) config.get( "applicationId" ), config, jobExecution );
            }
            catch ( Exception e ) {
                //if for any reason the backing up fails, then update the entity with a failed state.
                export.setState( Export.State.FAILED );
                em.update( export );
                return;
            }
        }
        export.setState( Export.State.FINISHED );
        em.update( export );
    }


    public SchedulerService getSch() {
        return sch;
    }


    public void setSch( final SchedulerService sch ) {
        this.sch = sch;
    }


    public EntityManagerFactory getEmf() {
        return emf;
    }


    public void setEmf( final EntityManagerFactory emf ) {
        this.emf = emf;
    }


    public ManagementService getManagementService() {

        return managementService;
    }


    public void setManagementService( final ManagementService managementService ) {
        this.managementService = managementService;
    }


    /**
     * Exports All Applications from an Organization
     */
    private void exportApplicationsFromOrg( UUID organizationUUID, final Map<String, Object> config,
                                            final JobExecution jobExecution ) throws Exception {

        //retrieves export entity
        UUID exportId = ( UUID ) jobExecution.getJobData().getProperty( EXPORT_ID );
        EntityManager exportManager = emf.getEntityManager( ( UUID ) config.get( "applicationId" ) );
        Export export = exportManager.get( exportId, Export.class );
        String appFileName = null;

        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( organizationUUID );

        for ( Map.Entry<UUID, String> application : applications.entrySet() ) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonGenerator jg = getJsonGenerator( baos );

            if ( application.getValue().equals(
                    managementService.getOrganizationByUuid( organizationUUID ).getName() + "/exporters" ) ) {
                continue;
            }

            appFileName = prepareOutputFileName( "application", application.getValue(), null );


            EntityManager em = emf.getEntityManager( application.getKey() );

            jg.writeStartArray();

            Map<String, Object> metadata = em.getApplicationCollectionMetadata();
            long starting_time = System.currentTimeMillis();

            // Loop through the collections. This is the only way to loop
            // through the entities in the application (former namespace).
            //could support queries, just need to implement that in the rest endpoint.
            for ( String collectionName : metadata.keySet() ) {
                if ( collectionName.equals( "exports" ) ) {
                    continue;
                }
                //if the collection you are looping through doesn't match the name of the one you want. Don't export it.

                if ( ( config.get( "collectionName" ) == null ) || collectionName
                        .equals( config.get( "collectionName" ) ) ) {
                    //Query entity manager for the entities in a collection
                    Query query;
                    if ( config.get( "query" ) == null ) {
                        query = new Query();
                    }
                    else {
                        query = Query.fromQL( ( String ) config.get( "query" ) );
                    }
                    query.setLimit( MAX_ENTITY_FETCH );
                    query.setResultsLevel( Results.Level.ALL_PROPERTIES );
                    Results entities = em.searchCollection( em.getApplicationRef(), collectionName, query );

                    //pages through the query and backs up all results.
                    PagingResultsIterator itr = new PagingResultsIterator( entities );
                    for ( Object e : itr ) {
                        starting_time = checkTimeDelta( starting_time, jobExecution );
                        Entity entity = ( Entity ) e;
                        jg.writeStartObject();
                        jg.writeFieldName( "Metadata" );
                        jg.writeObject( entity );
                        saveCollectionMembers( jg, em, ( String ) config.get( "collectionName" ), entity );
                        jg.writeEndObject();
                    }
                }
            }
            //}


            // Close writer and file for this application.
            jg.writeEndArray();
            jg.close();
            baos.flush();
            baos.close();

            //sets up the Inputstream for copying the method to s3.
            InputStream is = new ByteArrayInputStream( baos.toByteArray() );
            try {
                s3Export.copyToS3( is, config, appFileName );
            }
            catch ( Exception e ) {
                export.setState( Export.State.FAILED );
                return;
            }
        }
    }


    /**
     * Exports a specific applications from an organization
     */
    private void exportApplicationFromOrg( UUID organizationUUID, UUID applicationId, final Map<String, Object> config,
                                           final JobExecution jobExecution ) throws Exception {

        //retrieves export entity
        UUID exportId = ( UUID ) jobExecution.getJobData().getProperty( EXPORT_ID );
        EntityManager exportManager = emf.getEntityManager( ( UUID ) config.get( "applicationId" ) );
        Export export = exportManager.get( exportId, Export.class );

        //sets up a output stream for s3 backup.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ApplicationInfo application = managementService.getApplicationInfo( applicationId );
        String appFileName = prepareOutputFileName( "application", application.getName(), null );

        JsonGenerator jg = getJsonGenerator( baos );

        EntityManager em = emf.getEntityManager( applicationId );

        jg.writeStartArray();

        Map<String, Object> metadata = em.getApplicationCollectionMetadata();
        long starting_time = System.currentTimeMillis();

        // Loop through the collections. This is the only way to loop
        // through the entities in the application (former namespace).
        //could support queries, just need to implement that in the rest endpoint.
        for ( String collectionName : metadata.keySet() ) {
            if ( collectionName.equals( "exports" ) ) {
                continue;
            }
            //if the collection you are looping through doesn't match the name of the one you want. Don't export it.

            if ( ( config.get( "collectionName" ) == null ) || collectionName
                    .equals( config.get( "collectionName" ) ) ) {
                //Query entity manager for the entities in a collection
                Query query;
                if ( config.get( "query" ) == null ) {
                    query = new Query();
                }
                else {
                    query = Query.fromQL( ( String ) config.get( "query" ) );
                }
                // Query query = Query.fromQL( ( String ) config.get( "query" ) ); //new Query();
                query.setLimit( MAX_ENTITY_FETCH );
                query.setResultsLevel( Results.Level.ALL_PROPERTIES );
                query.setCollection( collectionName );
                //query.setQl( ( String ) config.get( "query" ) );

                Results entities = em.searchCollection( em.getApplicationRef(), collectionName, query );

                //pages through the query and backs up all results.
                PagingResultsIterator itr = new PagingResultsIterator( entities );
                for ( Object e : itr ) {
                    starting_time = checkTimeDelta( starting_time, jobExecution );
                    Entity entity = ( Entity ) e;
                    jg.writeStartObject();
                    jg.writeFieldName( "Metadata" );
                    jg.writeObject( entity );
                    saveCollectionMembers( jg, em, ( String ) config.get( "collectionName" ), entity );
                    jg.writeEndObject();
                }
            }
        }

        // Close writer and file for this application.
        jg.writeEndArray();
        jg.close();
        baos.flush();
        baos.close();

        //sets up the Inputstream for copying the method to s3.
        InputStream is = new ByteArrayInputStream( baos.toByteArray() );
        try {
            s3Export.copyToS3( is, config, appFileName );
        }
        catch ( Exception e ) {
            export.setState( Export.State.FAILED );
            return;
        }
    }


    /**
     * Exports a specific collection from an org-app combo.
     */
    //might be confusing, but uses the /s/ inclusion or exclusion nomenclature.
    private void exportCollectionFromOrgApp( UUID organizationUUID, UUID applicationUUID,
                                             final Map<String, Object> config, final JobExecution jobExecution )
            throws Exception {

        //retrieves export entity
        UUID exportId = ( UUID ) jobExecution.getJobData().getProperty( EXPORT_ID );
        EntityManager exportManager = emf.getEntityManager( ( UUID ) config.get( "applicationId" ) );
        Export export = exportManager.get( exportId, Export.class );

        //sets up a output stream for s3 backup.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ApplicationInfo application = managementService.getApplicationInfo( applicationUUID );

        JsonGenerator jg = getJsonGenerator( baos );

        EntityManager em = emf.getEntityManager( applicationUUID );

        jg.writeStartArray();

        Map<String, Object> metadata = em.getApplicationCollectionMetadata();
        long starting_time = System.currentTimeMillis();
        String appFileName = prepareOutputFileName( "application", application.getName(),
                ( String ) config.get( "collectionName" ) );

        // Loop through the collections. This is the only way to loop
        // through the entities in the application (former namespace).
        //could support queries, just need to implement that in the rest endpoint.
        for ( String collectionName : metadata.keySet() ) {
            //if the collection you are looping through doesn't match the name of the one you want. Don't export it.
            if ( collectionName.equals( ( String ) config.get( "collectionName" ) ) ) {
                //Query entity manager for the entities in a collection
                Query query;
                if ( config.get( "query" ) == null ) {
                    query = new Query();
                }
                else {
                    query = Query.fromQL( ( String ) config.get( "query" ) );
                }
                query.setLimit( MAX_ENTITY_FETCH );
                query.setResultsLevel( Results.Level.ALL_PROPERTIES );
                Results entities = em.searchCollection( em.getApplicationRef(), collectionName, query );

                //pages through the query and backs up all results.
                PagingResultsIterator itr = new PagingResultsIterator( entities );
                for ( Object e : itr ) {
                    starting_time = checkTimeDelta( starting_time, jobExecution );
                    Entity entity = ( Entity ) e;
                    jg.writeStartObject();
                    jg.writeFieldName( "Metadata" );
                    jg.writeObject( entity );
                    saveCollectionMembers( jg, em, ( String ) config.get( "collectionName" ), entity );
                    jg.writeEndObject();
                }
            }
        }

        // Close writer and file for this application.
        jg.writeEndArray();
        jg.close();
        baos.flush();
        baos.close();

        //sets up the Inputstream for copying the method to s3.
        InputStream is = new ByteArrayInputStream( baos.toByteArray() );


        try {
            s3Export.copyToS3( is, config, appFileName );
        }
        catch ( Exception e ) {
            export.setState( Export.State.FAILED );
            return;
        }
    }


    /**
     * Regulates how long to wait until the next heartbeat.
     */
    public long checkTimeDelta( long startingTime, final JobExecution jobExecution ) {

        long cur_time = System.currentTimeMillis();

        if ( startingTime <= ( cur_time - TIMESTAMP_DELTA ) ) {
            jobExecution.heartbeat();
            return cur_time;
        }
        return startingTime;
    }


    /**
     * Serialize and save the collection members of this <code>entity</code>
     *
     * @param em Entity Manager
     * @param collection Collection Name
     * @param entity entity
     */
    private void saveCollectionMembers( JsonGenerator jg, EntityManager em, String collection, Entity entity )
            throws Exception {

        Set<String> collections = em.getCollections( entity );

        // If your application doesn't have any e
        if ( ( collections == null ) || collections.isEmpty() ) {
            return;
        }

        for ( String collectionName : collections ) {

            if ( collectionName.equals( collection ) ) {
                jg.writeFieldName( collectionName );
                jg.writeStartArray();

                //is 100000 an arbitary number?
                Results collectionMembers =
                        em.getCollection( entity, collectionName, null, 100000, Results.Level.IDS, false );

                List<UUID> entityIds = collectionMembers.getIds();

                if ( ( entityIds != null ) && !entityIds.isEmpty() ) {
                    for ( UUID childEntityUUID : entityIds ) {
                        jg.writeObject( childEntityUUID.toString() );
                    }
                }

                // End collection array.
                jg.writeEndArray();
            }
        }

        // Write connections
        saveConnections( entity, em, jg );

        // Write dictionaries
        saveDictionaries( entity, em, jg );
    }


    /**
     * Persists the connection for this entity.
     */
    private void saveDictionaries( Entity entity, EntityManager em, JsonGenerator jg ) throws Exception {

        jg.writeFieldName( "dictionaries" );
        jg.writeStartObject();

        Set<String> dictionaries = em.getDictionaries( entity );
        for ( String dictionary : dictionaries ) {

            Map<Object, Object> dict = em.getDictionaryAsMap( entity, dictionary );

            // nothing to do
            if ( dict.isEmpty() ) {
                continue;
            }

            jg.writeFieldName( dictionary );

            jg.writeStartObject();

            for ( Map.Entry<Object, Object> entry : dict.entrySet() ) {
                jg.writeFieldName( entry.getKey().toString() );
                jg.writeObject( entry.getValue() );
            }

            jg.writeEndObject();
        }
        jg.writeEndObject();
    }


    /**
     * Persists the connection for this entity.
     */
    private void saveConnections( Entity entity, EntityManager em, JsonGenerator jg ) throws Exception {

        jg.writeFieldName( "connections" );
        jg.writeStartObject();

        Set<String> connectionTypes = em.getConnectionTypes( entity );
        for ( String connectionType : connectionTypes ) {

            jg.writeFieldName( connectionType );
            jg.writeStartArray();

            Results results = em.getConnectedEntities( entity.getUuid(), connectionType, null, Results.Level.IDS );
            List<ConnectionRef> connections = results.getConnections();

            for ( ConnectionRef connectionRef : connections ) {
                jg.writeObject( connectionRef.getConnectedEntity().getUuid() );
            }

            jg.writeEndArray();
        }
        jg.writeEndObject();
    }


    protected JsonGenerator getJsonGenerator( ByteArrayOutputStream out ) throws IOException {
        //TODO:shouldn't the below be UTF-16?
        //PrintWriter out = new PrintWriter( outFile, "UTF-8" );

        JsonGenerator jg = jsonFactory.createJsonGenerator( out );
        jg.setPrettyPrinter( new DefaultPrettyPrinter() );
        jg.setCodec( new ObjectMapper() );
        return jg;
    }


    /**
     * @param type just a label such us: organization, application.
     *
     * @return the file name concatenated with the type and the name of the collection
     */
    protected String prepareOutputFileName( String type, String name, String CollectionName ) {
        StringBuilder str = new StringBuilder();
        str.append( name );
        str.append( "." );
        if ( CollectionName != null ) {
            str.append( CollectionName );
            str.append( "." );
        }
        str.append( System.currentTimeMillis() );
        str.append( ".json" );

        String outputFileName = str.toString();

        return outputFileName;
    }


    @Autowired
    @Override
    public void setS3Export( S3Export s3Export ) { this.s3Export = s3Export; }
}
