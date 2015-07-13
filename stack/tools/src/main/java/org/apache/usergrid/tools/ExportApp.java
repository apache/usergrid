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
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.MinimalPrettyPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Export application's collections.
 */
public class ExportApp extends ExportingToolBase {
    static final Logger logger = LoggerFactory.getLogger( ExportApp.class );
    
    // we will write two types of files: entities and connections
    BlockingQueue<ExportEntity> entityWriteQueue = new LinkedBlockingQueue<ExportEntity>();
    BlockingQueue<ExportConnection> connectionWriteQueue = new LinkedBlockingQueue<ExportConnection>();

    static final String APPLICATION_NAME = "application";
    
    int pollTimeoutSeconds = 10;

    // limiting output threads will limit output files 
    final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(8);
    final Scheduler scheduler = Schedulers.from( threadPoolExecutor );

    Map<Thread, JsonGenerator> entityGeneratorsByThread  = new HashMap<Thread, JsonGenerator>();
    Map<Thread, JsonGenerator> connectionGeneratorsByThread = new HashMap<Thread, JsonGenerator>();

    List<String> emptyFiles = new ArrayList<String>();
    
    AtomicInteger activePollers = new AtomicInteger(0);
    AtomicInteger entitiesQueued = new AtomicInteger(0);
    AtomicInteger entitiesWritten = new AtomicInteger(0);
    AtomicInteger connectionsWritten = new AtomicInteger(0);
    AtomicInteger connectionsQueued = new AtomicInteger(0);

    ObjectMapper mapper = new ObjectMapper();
    
    /**
     * Export admin users using multiple threads.
     * <p/>
     * How it works:
     * In main thread we query for IDs of all admin users, add each ID to read queue.
     * Read-queue workers read admin user data, add data to write queue.
     * One write-queue worker reads data writes to file.
     */
    @Override
    public void runTool(CommandLine line) throws Exception {
        
        String applicationName = line.getOptionValue( APPLICATION_NAME );

//        if (StringUtils.isNotEmpty( line.getOptionValue( READ_THREAD_COUNT ) )) {
//            try {
//                readThreadCount = Integer.parseInt( line.getOptionValue( READ_THREAD_COUNT ) );
//            } catch (NumberFormatException nfe) {
//                logger.error( "-" + READ_THREAD_COUNT + " must be specified as an integer. Aborting..." );
//                return;
//            }
//        } else {
//            readThreadCount = 20;
//        }
        
        startSpring();

        setVerbose( line );

        applyOrgId( line );
        prepareBaseOutputFileName( line );
        outputDir = createOutputParentDir();
        logger.info( "Export directory: " + outputDir.getAbsolutePath() );
       
        UUID applicationId = emf.lookupApplication( applicationName );
        final EntityManager em = emf.getEntityManager( applicationId );

        // start write queue workers

        EntityWritesOnSubscribe entityWritesOnSub = new EntityWritesOnSubscribe( entityWriteQueue );
        rx.Observable entityWritesObservable = rx.Observable.create( entityWritesOnSub );
        entityWritesObservable.flatMap( new Func1<ExportEntity, Observable<?>>() {
            public Observable<ExportEntity> call(ExportEntity exportEntity) {
                return Observable.just(exportEntity).doOnNext( 
                        new EntityWriteAction() ).subscribeOn( scheduler );
            }
        },10).subscribeOn( scheduler ).subscribe();

        ConnectionWritesOnSubscribe connectionWritesOnSub = new ConnectionWritesOnSubscribe( connectionWriteQueue );
        rx.Observable connectionWritesObservable = rx.Observable.create( connectionWritesOnSub );
        connectionWritesObservable.flatMap( new Func1<ExportConnection, Observable<?>>() {
            public Observable<ExportConnection> call(ExportConnection connection ) {
                return Observable.just(connection).doOnNext( 
                        new ConnectionWriteAction()).subscribeOn( scheduler );
            }
        },10).subscribeOn( scheduler ).subscribe();

        // start processing data and filling up write queues

        CollectionsOnSubscribe onSubscribe = new CollectionsOnSubscribe( em );
        rx.Observable collectionsObservable = rx.Observable.create( onSubscribe );
        collectionsObservable.flatMap( new Func1<String, Observable<String>>() {
            public Observable<String> call(String collection) {
                return Observable.just(collection).doOnNext( 
                        new CollectionAction( em ) ).subscribeOn( Schedulers.io() );
            }
        },40).subscribeOn( Schedulers.io() ).subscribe();
        
        // wait for write thread pollers to get started

        try { Thread.sleep( 1000 ); } catch (InterruptedException ignored) {}

        // wait for write-thread pollers to stop

        while ( activePollers.get() > 0 ) {
            logger.info(
                     "Active write threads: {}\n"
                    +"Entities written:     {}\n"
                    +"Entities queued:      {}\n"
                    +"Connections written:  {}\n"
                    +"Connections queued:   {}\n",
                    new Object[] { 
                        activePollers.get(),
                        entitiesWritten.get(), 
                        entitiesQueued.get(),
                        connectionsWritten.get(), 
                        connectionsQueued.get()} );
            try { Thread.sleep( 5000 ); } catch (InterruptedException ignored) {}
        }

        // wrap up files

        for ( JsonGenerator gen : entityGeneratorsByThread.values() ) {
            //gen.writeEndArray();
            gen.flush();
            gen.close();
        }

        for ( JsonGenerator gen : connectionGeneratorsByThread.values() ) {
            //gen.writeEndArray();
            gen.flush();
            gen.close();
        }

        for ( String fileName : emptyFiles ) {
            File emptyFile = new File(fileName);
            emptyFile.deleteOnExit();
        }

    }

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option readThreads = OptionBuilder.hasArg().withType("")
                .withDescription( "Application Name -" + APPLICATION_NAME ).create( APPLICATION_NAME );
        options.addOption( readThreads );
        
//        Option readThreads = OptionBuilder
//                .hasArg().withType(0).withDescription("Read Threads -" + READ_THREAD_COUNT).create(READ_THREAD_COUNT);
//        options.addOption( readThreads );
        
        return options;
    }

    // ----------------------------------------------------------------------------------------
    // reading data

    /**
     * Emits collection names found in application.
     */
    class CollectionsOnSubscribe implements rx.Observable.OnSubscribe<String> {
        EntityManager em;
                
        public CollectionsOnSubscribe( EntityManager em ) {
            this.em = em;
        }

        public void call(Subscriber<? super String> subscriber) {
            
            logger.info("Starting to read collections");
            
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
            logger.info("Done. Read {} collection names", count);
            if ( count > 0 ) {
                subscriber.onCompleted();
            } else {
                subscriber.unsubscribe();
            }
        }
    }

    /**
     * Emits entities of collection.
     */
    class EntityOnSubscribe implements rx.Observable.OnSubscribe<ExportEntity> {
        EntityManager em;
        String collection;

        public EntityOnSubscribe(EntityManager em, String collection) {
            this.em = em;
            this.collection = collection;
        }

        public void call(Subscriber<? super ExportEntity> subscriber) {

            logger.info("Starting to read entities of collection {}", collection);
            
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
                                    em.getApplication().getApplicationName(), 
                                    entity, dictionariesByName );
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

                logger.info("Done. Read {} entities", count);
                if ( count > 0 ) {
                    subscriber.onCompleted();
                } else {
                    subscriber.unsubscribe();
                }
                
            } catch ( Exception e ) {
                subscriber.onError(e);
            }
        }
    }

    /**
     * Emits connections of an entity.
     */
    class ConnectionsOnSubscribe implements rx.Observable.OnSubscribe<ExportConnection> {
        EntityManager em;
        ExportEntity exportEntity;

        public ConnectionsOnSubscribe(EntityManager em, ExportEntity exportEntity) {
            this.em = em;
            this.exportEntity = exportEntity;
        }

        public void call(Subscriber<? super ExportConnection> subscriber) {

            logger.info("Starting to read connections for entity type {}", exportEntity.getEntity().getType());
            
            int count = 0;
            
            try {
                Set<String> connectionTypes = em.getConnectionTypes( exportEntity.getEntity() );
                for (String connectionType : connectionTypes) {

                    Results results = em.getConnectedEntities( 
                            exportEntity.getEntity().getUuid(), connectionType, null, Results.Level.CORE_PROPERTIES );

                    for (Entity connectedEntity : results.getEntities()) {
                        try {
                            ExportConnection connection = new ExportConnection( 
                                    em.getApplication().getApplicationName(), 
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

            logger.info("Done. Read {} connections", count);
            if ( count > 0 ) {
                subscriber.onCompleted();
            } else {
                subscriber.unsubscribe();
            }
        }
    }

    /**
     * Process collection by starting processing of its entities.
     */
    class CollectionAction implements Action1<String> {
        EntityManager em;

        public CollectionAction( EntityManager em ) {
            this.em = em;
        }

        public void call(String collection) {

            // process entities of collection in parallel            
            EntityOnSubscribe onSubscribe = new EntityOnSubscribe( em, collection );
            rx.Observable entityObservable = rx.Observable.create( onSubscribe );
            entityObservable.flatMap( new Func1<ExportEntity, Observable<ExportEntity>>() {
                public Observable<ExportEntity> call(ExportEntity exportEntity) {
                    return Observable.just(exportEntity).doOnNext( 
                            new EntityAction( em ) ).subscribeOn( Schedulers.io() );
                }
            }, 8).subscribeOn(Schedulers.io()).toBlocking().last();
        }
    }

    /**
     * Process entity by adding it to entityWriteQueue and starting processing of its connections.
     */
    class EntityAction implements Action1<ExportEntity> {
        EntityManager em;

        public EntityAction( EntityManager em ) {
            this.em = em;
        }
        
        public void call(ExportEntity exportEntity) {
            //logger.debug( "Processing entity: " + exportEntity.getEntity().getUuid() );

            entityWriteQueue.add( exportEntity );
            entitiesQueued.getAndIncrement();

            // if entity has connections, process them in parallel
            try {
                Results connectedEntities = em.getConnectedEntities(
                        exportEntity.getEntity().getUuid(), null, null, Results.Level.CORE_PROPERTIES );

                if ( !connectedEntities.isEmpty() ) {
                    ConnectionsOnSubscribe onSubscribe = new ConnectionsOnSubscribe( em, exportEntity );
                    rx.Observable entityObservable = rx.Observable.create( onSubscribe );
                    
                    entityObservable.flatMap( new Func1<ExportConnection, Observable<ExportConnection>>() {
                        public Observable<ExportConnection> call(ExportConnection connection) {
                            return Observable.just(connection).doOnNext( 
                                    new ConnectionsAction() ).subscribeOn( Schedulers.io() );
                        }
                    }, 8).subscribeOn(Schedulers.io()).toBlocking().last();
                }
                
            } catch (Exception e) {
                throw new RuntimeException( "Error getting connections", e );
            }
        }
    }

    /**
     * Process connection by adding it to connectionWriteQueue. 
     */
    class ConnectionsAction implements Action1<ExportConnection> {

        public void call(ExportConnection conn) {
            //logger.debug( "Processing connections for entity: " + conn.getSourceUuid() );
            connectionWriteQueue.add(conn);
            connectionsQueued.getAndIncrement();
        }
    }


    // ----------------------------------------------------------------------------------------
    // writing data

    /**
     * Emits entities to be written.
     */
    class EntityWritesOnSubscribe implements rx.Observable.OnSubscribe<ExportEntity> {
        BlockingQueue<ExportEntity> queue;

        public EntityWritesOnSubscribe( BlockingQueue<ExportEntity> queue ) {
            this.queue = queue;
        }

        public void call(Subscriber<? super ExportEntity> subscriber) {
            int count = 0;
            
            while ( true ) {
                ExportEntity entity = null;
                try {
                    //logger.debug( "Wrote {}. Polling for entity to write...", count );
                    activePollers.getAndIncrement();
                    entity = queue.poll( pollTimeoutSeconds, TimeUnit.SECONDS );
                } catch (InterruptedException e) {
                    logger.error("Entity poll interrupted", e);
                    continue;
                } finally {
                    activePollers.getAndDecrement();
                }
                if ( entity == null ) {
                    break;
                }
                subscriber.onNext( entity );
                count++;
            }

            logger.info("Done. Wrote {} entities", count);
            if ( count > 0 ) {
                subscriber.onCompleted();
            } else {
                subscriber.unsubscribe();
            }
        }
    }

    /**
     * Emits connections to be written.
     */
    class ConnectionWritesOnSubscribe implements rx.Observable.OnSubscribe<ExportConnection> {
        BlockingQueue<ExportConnection> queue;

        public ConnectionWritesOnSubscribe( BlockingQueue<ExportConnection> queue ) {
            this.queue = queue;
        }

        public void call(Subscriber<? super ExportConnection> subscriber) {
            int count = 0;
            
            while ( true ) {
                ExportConnection connection = null;
                try {
                    //logger.debug( "Wrote {}. Polling for connection to write", count );
                    activePollers.getAndIncrement();
                    connection = queue.poll( pollTimeoutSeconds, TimeUnit.SECONDS );
                } catch (InterruptedException e) {
                    logger.error("Connection poll interrupted", e);
                    continue;
                } finally {
                    activePollers.getAndDecrement();
                }
                if ( connection == null ) {
                    break;
                }
                subscriber.onNext( connection );
                count++;
            }

            logger.info("Done. Wrote {} connections", count);
            if ( count > 0 ) {
                subscriber.onCompleted();
            } else {
                subscriber.unsubscribe();
            }
        }
    }

    /**
     * Writes entities to JSON file.
     */
    class EntityWriteAction implements Action1<ExportEntity> {

        public void call(ExportEntity entity) {

            boolean wroteData = false;

            String fileName = "target/" + Thread.currentThread().getName() + ".ude";

            JsonGenerator gen = entityGeneratorsByThread.get( Thread.currentThread() );
            if ( gen == null ) {

                // no generator so we are opening new file and writing the start of an array
                try {
                    gen = jsonFactory.createJsonGenerator( new FileOutputStream( fileName ) );
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
                wroteData = true;

            } catch (IOException e) {
                throw new RuntimeException("Error writing to output file: " + fileName, e);
            }

            if ( !wroteData ) {
                emptyFiles.add( fileName );
            }
        }
    }

    /**
     * Writes connection to JSON file.
     */
    class ConnectionWriteAction implements Action1<ExportConnection> {

        public void call(ExportConnection conn) {

            boolean wroteData = false;

            String fileName = "target/" + Thread.currentThread().getName() + ".ugc";

            JsonGenerator gen = connectionGeneratorsByThread.get( Thread.currentThread() );
            if ( gen == null ) {

                // no generator so we are opening new file and writing the start of an array
                try {
                    gen = jsonFactory.createJsonGenerator( new FileOutputStream( fileName ) );
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
                wroteData = true;

            } catch (IOException e) {
                throw new RuntimeException("Error writing to output file: " + fileName, e);
            }

            if ( !wroteData ) {
                emptyFiles.add( fileName );
            }
        }
    }

}

class ExportEntity {
    private String application;
    private Entity entity;
    private Map<String, Object> dictionaries;
    public ExportEntity( String application, Entity entity, Map<String, Object> dictionaries ) {
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
}

class ExportConnection {
    private String application;
    private String connectionType;
    private UUID sourceUuid;
    private UUID targetUuid;
    public ExportConnection(String application, String connectionType, UUID sourceUuid, UUID targetUuid) {
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
}
