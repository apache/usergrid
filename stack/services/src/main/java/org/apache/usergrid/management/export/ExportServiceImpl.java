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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;


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


    @Override
    public UUID schedule( final Map<String, Object> config ) throws Exception {
        ApplicationInfo defaultExportApp = null;

        if ( config == null ) {
            logger.error( "export information cannot be null" );
            return null;
        }

        EntityManager em = null;
        try {
            em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );
            Set<String> collections = em.getApplicationCollections();
            if ( !collections.contains( "exports" ) ) {
                em.createApplicationCollection( "exports" );
            }
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
    public String getState( final UUID uuid ) throws Exception {

        if ( uuid == null ) {
            logger.error( "UUID passed in cannot be null." );
            return "UUID passed in cannot be null";
        }

        EntityManager rootEm = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        //retrieve the export entity.
        Export export = rootEm.get( uuid, Export.class );

        if ( export == null ) {
            logger.error( "no entity with that uuid was found" );
            return "No Such Element found";
        }
        return export.getState().toString();
    }


    @Override
    public String getErrorMessage( final UUID appId, final UUID uuid ) throws Exception {

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
        return export.getErrorMessage();
    }


    @Override
    public void doExport( final JobExecution jobExecution ) throws Exception {
        Map<String, Object> config = ( Map<String, Object> ) jobExecution.getJobData().getProperty( "exportInfo" );
        Object s3PlaceHolder = jobExecution.getJobData().getProperty( "s3Export" );
        S3Export s3Export = null;

        if ( config == null ) {
            logger.error( "Export Information passed through is null" );
            return;
        }
        //get the entity manager for the application, and the entity that this Export corresponds to.
        UUID exportId = ( UUID ) jobExecution.getJobData().getProperty( EXPORT_ID );

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );
        Export export = em.get( exportId, Export.class );

        //update the entity state to show that the job has officially started.
        export.setState( Export.State.STARTED );
        em.update( export );
        try {
            if ( s3PlaceHolder != null ) {
                s3Export = ( S3Export ) s3PlaceHolder;
            }
            else {
                s3Export = new S3ExportImpl();
            }
        }
        catch ( Exception e ) {
            logger.error( "S3Export doesn't exist" );
            export.setErrorMessage( e.getMessage() );
            export.setState( Export.State.FAILED );
            em.update( export );
            return;
        }

        if ( config.get( "organizationId" ) == null ) {
            logger.error( "No organization could be found" );
            export.setState( Export.State.FAILED );
            em.update( export );
            return;
        }
        else if ( config.get( "applicationId" ) == null ) {
            //exports All the applications from an organization
            try {
                exportApplicationsFromOrg( ( UUID ) config.get( "organizationId" ), config, jobExecution, s3Export );
            }
            catch ( Exception e ) {
                export.setErrorMessage( e.getMessage() );
                export.setState( Export.State.FAILED );
                em.update( export );
                return;
            }
        }
        else if ( config.get( "collectionName" ) == null ) {
            //exports an Application from a single organization
            try {
                exportApplicationFromOrg( ( UUID ) config.get( "organizationId" ),
                        ( UUID ) config.get( "applicationId" ), config, jobExecution, s3Export );
            }
            catch ( Exception e ) {
                export.setErrorMessage( e.getMessage() );
                export.setState( Export.State.FAILED );
                em.update( export );
                return;
            }
        }
        else {
            try {
                //exports a single collection from an app org combo
                try {
                    exportCollectionFromOrgApp( ( UUID ) config.get( "applicationId" ), config, jobExecution,
                            s3Export );
                }
                catch ( Exception e ) {
                    export.setErrorMessage( e.getMessage() );
                    export.setState( Export.State.FAILED );
                    em.update( export );
                    return;
                }
            }
            catch ( Exception e ) {
                //if for any reason the backing up fails, then update the entity with a failed state.
                export.setErrorMessage( e.getMessage() );
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


    public Export getExportEntity( final JobExecution jobExecution ) throws Exception {

        UUID exportId = ( UUID ) jobExecution.getJobData().getProperty( EXPORT_ID );
        EntityManager exportManager = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        return exportManager.get( exportId, Export.class );
    }


    /**
     * Exports All Applications from an Organization
     */
    private void exportApplicationsFromOrg( UUID organizationUUID, final Map<String, Object> config,
                                            final JobExecution jobExecution, S3Export s3Export ) throws Exception {

        //retrieves export entity
        Export export = getExportEntity( jobExecution );
        String appFileName = null;

        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( organizationUUID );

        for ( Map.Entry<UUID, String> application : applications.entrySet() ) {

            if ( application.getValue().equals(
                    managementService.getOrganizationByUuid( organizationUUID ).getName() + "/exports" ) ) {
                continue;
            }

            appFileName = prepareOutputFileName( "application", application.getValue(), null );

            File ephemeral = collectionExportAndQuery( application.getKey(), config, export, jobExecution );

            fileTransfer( export, appFileName, ephemeral, config, s3Export );
        }
    }


    public void fileTransfer( Export export, String appFileName, File ephemeral, Map<String, Object> config,
                              S3Export s3Export ) {
        try {
            s3Export.copyToS3( ephemeral, config, appFileName );

        }
        catch ( Exception e ) {
            export.setErrorMessage( e.getMessage() );
            export.setState( Export.State.FAILED );
            return;
        }
    }


    /**
     * Exports a specific applications from an organization
     */
    private void exportApplicationFromOrg( UUID organizationUUID, UUID applicationId, final Map<String, Object> config,
                                           final JobExecution jobExecution, S3Export s3Export ) throws Exception {

        //retrieves export entity
        Export export = getExportEntity( jobExecution );

        ApplicationInfo application = managementService.getApplicationInfo( applicationId );
        String appFileName = prepareOutputFileName( "application", application.getName(), null );

        File ephemeral = collectionExportAndQuery( applicationId, config, export, jobExecution );

        fileTransfer( export, appFileName, ephemeral, config, s3Export );
    }


    /**
     * Exports a specific collection from an org-app combo.
     */
    //might be confusing, but uses the /s/ inclusion or exclusion nomenclature.
    private void exportCollectionFromOrgApp( UUID applicationUUID, final Map<String, Object> config,
                                             final JobExecution jobExecution, S3Export s3Export ) throws Exception {

        //retrieves export entity
        Export export = getExportEntity( jobExecution );
        ApplicationInfo application = managementService.getApplicationInfo( applicationUUID );

        String appFileName = prepareOutputFileName( "application", application.getName(),
                ( String ) config.get( "collectionName" ) );


        File ephemeral = collectionExportAndQuery( applicationUUID, config, export, jobExecution );

        fileTransfer( export, appFileName, ephemeral, config, s3Export );
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


    protected JsonGenerator getJsonGenerator( File ephermal ) throws IOException {
        //TODO:shouldn't the below be UTF-16?

        JsonGenerator jg = jsonFactory.createJsonGenerator( ephermal, JsonEncoding.UTF8 );
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


    /**
     * handles the query and export of collections
     */
    //TODO:Needs further refactoring.
    protected File collectionExportAndQuery( UUID applicationUUID, final Map<String, Object> config, Export export,
                                             final JobExecution jobExecution ) throws Exception {

        EntityManager em = emf.getEntityManager( applicationUUID );
        Map<String, Object> metadata = em.getApplicationCollectionMetadata();
        long starting_time = System.currentTimeMillis();
        File ephemeral = new File( "tempExport" + UUID.randomUUID() );
        ephemeral.deleteOnExit();


        JsonGenerator jg = getJsonGenerator( ephemeral );

        jg.writeStartArray();

        for ( String collectionName : metadata.keySet() ) {
            if ( collectionName.equals( "exports" ) ) {
                continue;
            }
            //if the collection you are looping through doesn't match the name of the one you want. Don't export it.

            if ( ( config.get( "collectionName" ) == null ) || collectionName
                    .equals( config.get( "collectionName" ) ) ) {
                //Query entity manager for the entities in a collection
                Query query = null;
                if ( config.get( "query" ) == null ) {
                    query = new Query();
                }
                else {
                    try {
                        query = Query.fromQL( ( String ) config.get( "query" ) );
                    }
                    catch ( Exception e ) {
                        export.setErrorMessage( e.getMessage() );
                    }
                }
                query.setLimit( MAX_ENTITY_FETCH );
                query.setResultsLevel( Results.Level.ALL_PROPERTIES );
                query.setCollection( collectionName );

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
                    jg.flush();
                }
            }
        }
        jg.writeEndArray();
        jg.flush();
        jg.close();

        return ephemeral;
    }
}
