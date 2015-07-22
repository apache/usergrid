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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.utils.StringUtils;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.MinimalPrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Export all entities and connections of a Usergrid app.
 * 
 * Exports data files to specified directory.
 * 
 * Will create as many output files as there are writeThreads (by default: 10).
 * 
 * Will create two types of files: *.entities for Usegrird entities and *.collections for entity to entity connections.
 * 
 * Every line of the data files is a complete JSON object.
 */
public class ExportApp extends ExportingToolBase {
    static final Logger logger = LoggerFactory.getLogger( ExportApp.class );

    static final String APPLICATION_NAME = "application";
    private static final String WRITE_THREAD_COUNT = "writeThreads";
   
    String applicationName;
    String organizationName;

    AtomicInteger entitiesWritten = new AtomicInteger(0);
    AtomicInteger connectionsWritten = new AtomicInteger(0);

    Scheduler writeScheduler;

    ObjectMapper mapper = new ObjectMapper();
    Map<Thread, JsonGenerator> entityGeneratorsByThread  = new HashMap<Thread, JsonGenerator>();
    Map<Thread, JsonGenerator> connectionGeneratorsByThread = new HashMap<Thread, JsonGenerator>();

    int writeThreadCount = 10; // set via CLI option; limiting write will limit output files 


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option appNameOption = OptionBuilder.hasArg().withType("")
                .withDescription( "Application Name -" + APPLICATION_NAME ).create( APPLICATION_NAME );
        options.addOption( appNameOption );

        Option writeThreadsOption = OptionBuilder.hasArg().withType(0)
                .withDescription( "Write Threads -" + WRITE_THREAD_COUNT ).create(WRITE_THREAD_COUNT);
        options.addOption( writeThreadsOption );

        return options;
    }

    
    /**
     * Tool entry point. 
     */
    @Override
    public void runTool(CommandLine line) throws Exception {

        applicationName = line.getOptionValue( APPLICATION_NAME );

        if (StringUtils.isNotEmpty( line.getOptionValue( WRITE_THREAD_COUNT ) )) {
            try {
                writeThreadCount = Integer.parseInt( line.getOptionValue( WRITE_THREAD_COUNT ) );
            } catch (NumberFormatException nfe) {
                logger.error( "-" + WRITE_THREAD_COUNT + " must be specified as an integer. Aborting..." );
                return;
            }
        }

        setVerbose( line );

        applyOrgId( line );
        prepareBaseOutputFileName( line );
        outputDir = createOutputParentDir();
        logger.info( "Export directory: " + outputDir.getAbsolutePath() );

        startSpring();

        UUID applicationId = emf.lookupApplication( applicationName );
        if (applicationId == null) {
            throw new RuntimeException( "Cannot find application " + applicationName );
        }
        final EntityManager em = emf.getEntityManager( applicationId );
        organizationName = em.getApplication().getOrganizationName();

        ExecutorService writeThreadPoolExecutor = Executors.newFixedThreadPool( writeThreadCount );
        writeScheduler = Schedulers.from( writeThreadPoolExecutor );

        Observable<String> collectionsObservable = Observable.create( new CollectionsObservable( em ) );
        
        collectionsObservable.flatMap( new Func1<String, Observable<ExportEntity>>() {

            public Observable<ExportEntity> call(String collection) {

                return Observable.create( new EntityObservable( em, collection ) )
                        .doOnNext( new EntityWriteAction() ).subscribeOn( writeScheduler );
            }

        }, writeThreadCount ).flatMap( new Func1<ExportEntity, Observable<ExportConnection>>() {

            public Observable<ExportConnection> call(ExportEntity exportEntity) {

                return Observable.create( new ConnectionsObservable( em, exportEntity ) )
                        .doOnNext( new ConnectionWriteAction() ).subscribeOn( writeScheduler );
            }

        }, writeThreadCount )
            .doOnCompleted( new FileWrapUpAction() )
            .toBlocking().last();
    }
   
    
    // ----------------------------------------------------------------------------------------
    // reading data

    
    /**
     * Emits collection names found in application.
     */
    class CollectionsObservable implements rx.Observable.OnSubscribe<String> {
        EntityManager em;
                
        public CollectionsObservable(EntityManager em) {
            this.em = em;
        }

        public void call(Subscriber<? super String> subscriber) {
            
            int count = 0;
            try {
                Map<String, Object> collectionMetadata = em.getApplicationCollectionMetadata();
                for ( String collection : collectionMetadata.keySet() ) {
                    subscriber.onNext( collection );
                    count++;
                }
                
            } catch (Exception e) {
                subscriber.onError( e );
            }
            
            subscriber.onCompleted();
            logger.info( "Completed. Read {} collection names", count );
        }
    }

    
    /**
     * Emits entities of collection.
     */
    class EntityObservable implements rx.Observable.OnSubscribe<ExportEntity> {
        EntityManager em;
        String collection;

        public EntityObservable(EntityManager em, String collection) {
            this.em = em;
            this.collection = collection;
        }

        public void call(Subscriber<? super ExportEntity> subscriber) {

            logger.info("Starting to read entities of collection {}", collection);
            
            subscriber.onStart();
            
            try {
                int count = 0;

                Query query = new Query();
                query.setLimit( MAX_ENTITY_FETCH );

                Results results = em.searchCollection( em.getApplicationRef(), collection, query );

                while (results.size() > 0) {
                    for (Entity entity : results.getEntities()) {
                        try {
                            Set<String> dictionaries = em.getDictionaries( entity );
                            Map dictionariesByName = new HashMap<String, Map<Object, Object>>();
                            for (String dictionary : dictionaries) {
                                Map<Object, Object> dict = em.getDictionaryAsMap( entity, dictionary );
                                if (dict.isEmpty()) {
                                    continue;
                                }
                                dictionariesByName.put( dictionary, dict );
                            }
                            
                            ExportEntity exportEntity = new ExportEntity( 
                                    organizationName, 
                                    applicationName, 
                                    entity, 
                                    dictionariesByName );
                            
                            subscriber.onNext( exportEntity );
                            count++;
                            
                        } catch (Exception e) {
                            logger.error("Error reading entity " + entity.getUuid() +" from collection " + collection);
                        }
                    }
                    if (results.getCursor() == null) {
                        break;
                    }
                    query.setCursor( results.getCursor() );
                    results = em.searchCollection( em.getApplicationRef(), collection, query );
                }

                subscriber.onCompleted();
                logger.info("Completed collection {}. Read {} entities", collection, count);
                
            } catch ( Exception e ) {
                subscriber.onError(e);
            }
        }
    }

    
    /**
     * Emits connections of an entity.
     */
    class ConnectionsObservable implements rx.Observable.OnSubscribe<ExportConnection> {
        EntityManager em;
        ExportEntity exportEntity;

        public ConnectionsObservable(EntityManager em, ExportEntity exportEntity) {
            this.em = em;
            this.exportEntity = exportEntity;
        }

        public void call(Subscriber<? super ExportConnection> subscriber) {

            logger.info( "Starting to read connections for entity {} type {}",
                    exportEntity.getEntity().getName(), exportEntity.getEntity().getType() );
            
            int count = 0;
            
            try {
                Set<String> connectionTypes = em.getConnectionTypes( exportEntity.getEntity() );
                for (String connectionType : connectionTypes) {

                    Results results = em.getConnectedEntities( 
                            exportEntity.getEntity().getUuid(), connectionType, null, Results.Level.CORE_PROPERTIES );

                    for (Entity connectedEntity : results.getEntities()) {
                        try {
                            
                            ExportConnection connection = new ExportConnection( 
                                    applicationName,
                                    organizationName,
                                    connectionType, 
                                    exportEntity.getEntity().getUuid(), 
                                    connectedEntity.getUuid());
                            
                            subscriber.onNext( connection );
                            count++;

                        } catch (Exception e) {
                            logger.error( "Error reading connection entity " 
                                + exportEntity.getEntity().getUuid() + " -> " + connectedEntity.getType());
                        }
                    }
                }
                
            } catch (Exception e) {
                subscriber.onError( e );
            }
            
            subscriber.onCompleted();
            logger.info("Completed entity {} type {} connections count {}",
                new Object[] { exportEntity.getEntity().getName(), exportEntity.getEntity().getType(), count });
        }
    }

    
    // ----------------------------------------------------------------------------------------
    // writing data
    
    
    /**
     * Writes entities to JSON file.
     */
    class EntityWriteAction implements Action1<ExportEntity> {

        public void call(ExportEntity entity) {

            String [] parts = Thread.currentThread().getName().split("-");
            String fileName = outputDir.getAbsolutePath() + File.separator
                    + applicationName.replace('/','-') + "-" + parts[3] + ".entities";

            JsonGenerator gen = entityGeneratorsByThread.get( Thread.currentThread() );
            if ( gen == null ) {

                // no generator so we are opening new file and writing the start of an array
                try {
                    gen = jsonFactory.createJsonGenerator( new FileOutputStream( fileName ) );
                    logger.info("Opened output file {}", fileName);
                } catch (IOException e) {
                    throw new RuntimeException("Error opening output file: " + fileName, e);
                }
                gen.setPrettyPrinter( new MinimalPrettyPrinter(""));
                gen.setCodec( mapper );
                entityGeneratorsByThread.put( Thread.currentThread(), gen );
            }

            try {
                gen.writeObject( entity );
                gen.writeRaw('\n');
                entitiesWritten.getAndIncrement();

            } catch (IOException e) {
                throw new RuntimeException("Error writing to output file: " + fileName, e);
            }
        }
    }

    
    /**
     * Writes connection to JSON file.
     */
    class ConnectionWriteAction implements Action1<ExportConnection> {

        public void call(ExportConnection conn) {

            String [] parts = Thread.currentThread().getName().split("-");
            String fileName = outputDir.getAbsolutePath() + File.separator
                    + applicationName.replace('/','-') + "-" + parts[3] + ".connections";

            JsonGenerator gen = connectionGeneratorsByThread.get( Thread.currentThread() );
            if ( gen == null ) {

                // no generator so we are opening new file and writing the start of an array
                try {
                    gen = jsonFactory.createJsonGenerator( new FileOutputStream( fileName ) );
                    logger.info("Opened output file {}", fileName);
                } catch (IOException e) {
                    throw new RuntimeException("Error opening output file: " + fileName, e);
                }
                gen.setPrettyPrinter( new MinimalPrettyPrinter(""));
                gen.setCodec( mapper );
                connectionGeneratorsByThread.put( Thread.currentThread(), gen );
            }

            try {
                gen.writeObject( conn );
                gen.writeRaw('\n');
                connectionsWritten.getAndIncrement();

            } catch (IOException e) {
                throw new RuntimeException("Error writing to output file: " + fileName, e);
            }
        }
    }

    
    private class FileWrapUpAction implements Action0 {
        @Override
        public void call() {

            logger.info("-------------------------------------------------------------------");
            logger.info("DONE! Entities: {} Connections: {}", entitiesWritten.get(), connectionsWritten.get());
            logger.info("-------------------------------------------------------------------");

            for ( JsonGenerator gen : entityGeneratorsByThread.values() ) {
                try {
                    //gen.writeEndArray();
                    gen.flush();
                    gen.close();
                } catch (IOException e) {
                    logger.error("Error closing output file", e);
                }
            }
            for ( JsonGenerator gen : connectionGeneratorsByThread.values() ) {
                try {
                    //gen.writeEndArray();
                    gen.flush();
                    gen.close();
                } catch (IOException e) {
                    logger.error("Error closing output file", e);
                }
            }
        }
    }
}


/**
 * Represents entity data to be serialized to JSON.
 */
class ExportEntity {
    private String organization;
    private String application;
    private Entity entity;
    private Map<String, Object> dictionaries;
    public ExportEntity( String organization, String application, Entity entity, Map<String, Object> dictionaries ) {
        this.organization = organization;
        this.application = application;
        this.entity = entity;
        this.dictionaries = dictionaries;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Map<String, Object> getDictionaries() {
        return dictionaries;
    }

    public void setDictionaries(Map<String, Object> dictionaries) {
        this.dictionaries = dictionaries;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}


/**
 * Represents connection data to be serialized to JSON.
 */
class ExportConnection {
    private String organization;
    private String application;
    private String connectionType;
    private UUID sourceUuid;
    private UUID targetUuid;
    public ExportConnection(String organization, String application, String connectionType, UUID sourceUuid, UUID targetUuid) {
        this.organization= organization;
        this.application = application;
        this.connectionType = connectionType;
        this.sourceUuid = sourceUuid;
        this.targetUuid = targetUuid;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(UUID sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}
