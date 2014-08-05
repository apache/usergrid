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

package org.apache.usergrid.management.importUG;

        import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.service.SchedulerService;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
        import org.apache.usergrid.persistence.*;
        import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.entities.JobData;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
public class ImportServiceImpl implements ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);
    public static final String IMPORT_ID = "importId";
    public static final String IMPORT_JOB_NAME = "importJob";
    private ArrayList<File> files;

    //dependency injection
    private SchedulerService sch;

    //injected the Entity Manager Factory
    protected EntityManagerFactory emf;

    //inject Management Service to access Organization Data
    private ManagementService managementService;

    //Amount of time that has passed before sending another heart beat in millis
    public static final int TIMESTAMP_DELTA = 5000;

    private JsonFactory jsonFactory = new JsonFactory();

    private int entityCount=0;

    /**
     *
     * @param config configuration of the job to be scheduled
     * @return it returns the UUID of the scheduled job
     * @throws Exception
     */
    @Override
    public UUID schedule(Map<String, Object> config) throws Exception {

        ApplicationInfo defaultImportApp = null;

        if ( config == null ) {
            logger.error( "import information cannot be null" );
            return null;
        }

        EntityManager em = null;
        try {
            em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );
            Set<String> collections = em.getApplicationCollections();
            if ( !collections.contains( "imports" ) ) {
                em.createApplicationCollection( "imports" );
            }
        }
        catch ( Exception e ) {
            logger.error( "application doesn't exist within the current context" );
            return null;
        }

        Import importUG = new Import();

        //update state
        try {
            importUG = em.create( importUG );
        }
        catch ( Exception e ) {
            logger.error( "Import entity creation failed" );
            return null;
        }

        importUG.setState( Import.State.CREATED );
        em.update( importUG );

        //set data to be transferred to importInfo
        JobData jobData = new JobData();
        jobData.setProperty( "importInfo", config );
        jobData.setProperty( IMPORT_ID, importUG.getUuid() );

        long soonestPossible = System.currentTimeMillis() + 250; //sch grace period

        //schedule job
        sch.createJob( IMPORT_JOB_NAME, soonestPossible, jobData );

        //update state
        importUG.setState( Import.State.SCHEDULED );
        em.update( importUG );

        return importUG.getUuid();
    }



    /**
     * Query Entity Manager for the string state of the Import Entity. This corresponds to the GET /import
     *
     * @return String
     */
    @Override
    public String getState(UUID uuid) throws Exception {
        if ( uuid == null ) {
            logger.error( "UUID passed in cannot be null." );
            return "UUID passed in cannot be null";
        }

        EntityManager rootEm = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        //retrieve the import entity.
        Import importUG = rootEm.get( uuid, Import.class );

        if ( importUG == null ) {
            logger.error( "no entity with that uuid was found" );
            return "No Such Element found";
        }
        return importUG.getState().toString();
    }

    /**
     * Query Entity Manager for the error message generated for an import job.
     *
     * @return String
     */
    @Override
    public String getErrorMessage(final UUID uuid ) throws Exception {

        //get application entity manager

        if ( uuid == null ) {
            logger.error( "UUID passed in cannot be null." );
            return "UUID passed in cannot be null";
        }

        EntityManager rootEm = emf.getEntityManager(  MANAGEMENT_APPLICATION_ID );

        //retrieve the import entity.
        Import importUG = rootEm.get( uuid, Import.class );

        if ( importUG == null ) {
            logger.error( "no entity with that uuid was found" );
            return "No Such Element found";
        }
        return importUG.getErrorMessage().toString();
    }


    //@Override
    public Import getImportEntity( final JobExecution jobExecution ) throws Exception {

        UUID importId = ( UUID ) jobExecution.getJobData().getProperty( IMPORT_ID );
        EntityManager importManager = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);

        return importManager.get( importId, Import.class );
    }


    @Override
    public ArrayList<File> getEphemeralFile() {
        return files;
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
     *
     * @param jobExecution the job created by the scheduler with all the required config data
     * @throws Exception
     */
    @Override
    public void doImport(JobExecution jobExecution) throws Exception {

        Map<String, Object> config = (Map<String, Object>) jobExecution.getJobData().getProperty("importInfo");
        Object s3PlaceHolder = jobExecution.getJobData().getProperty("s3Import");
        S3Import s3Import = null;

        if (config == null) {
            logger.error("Import Information passed through is null");
            return;
        }

        //get the entity manager for the application, and the entity that this Import corresponds to.
        UUID importId = (UUID) jobExecution.getJobData().getProperty(IMPORT_ID);

        EntityManager em = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);
        Import importUG = em.get(importId, Import.class);

        //update the entity state to show that the job has officially started.
        importUG.setState(Import.State.STARTED);
        em.update(importUG);
        try {
            if (s3PlaceHolder != null) {
                s3Import = (S3Import) s3PlaceHolder;
            } else {
                s3Import = new S3ImportImpl();
            }
        } catch (Exception e) {
            logger.error("S3Import doesn't exist");
            importUG.setErrorMessage(e.getMessage());
            importUG.setState(Import.State.FAILED);
            em.update(importUG);
            return;
        }

        try {
            if (config.get("organizationId") == null) {
                logger.error("No organization could be found");
                importUG.setState(Import.State.FAILED);
                em.update(importUG);
                return;
            } else if (config.get("applicationId") == null) {
                //import All the applications from an organization
                importApplicationsFromOrg((UUID) config.get("organizationId"), config, jobExecution, s3Import);
            } else if (config.get("collectionName") == null) {
                //imports an Application from a single organization
                importApplicationFromOrg((UUID) config.get("organizationId"), (UUID) config.get("applicationId"), config, jobExecution, s3Import);
            } else {
                //imports a single collection from an app org combo
                importCollectionFromOrgApp((UUID) config.get("applicationId"), config, jobExecution, s3Import);
            }
        }
        catch (OrganizationNotFoundException e) {
            importUG.setErrorMessage(e.getMessage());
            importUG.setState(Import.State.FAILED);
            em.update(importUG);
            return;
        }
        catch (ApplicationNotFoundException e) {
            importUG.setErrorMessage(e.getMessage());
            importUG.setState(Import.State.FAILED);
            em.update(importUG);
            return;
        }
        catch (Exception e) {
            // the case where job will be retried i.e. resumed from the failed point
            importUG.setErrorMessage(e.getMessage());
            importUG.setState(Import.State.FAILED);
            em.update(importUG);
            throw e;
        }

        importUG.setState( Import.State.FINISHED );
        em.update( importUG );
    }

    /**
     * Imports a specific collection from an org-app combo.
     */
    private void importCollectionFromOrgApp( UUID applicationUUID, final Map<String, Object> config,
                                             final JobExecution jobExecution, S3Import s3Import ) throws Exception {

        //retrieves export entity
        Import importUG = getImportEntity(jobExecution);
        ApplicationInfo application = managementService.getApplicationInfo(applicationUUID);

        if(application == null) {
            throw new ApplicationNotFoundException("Application Not Found");
        }
        String collectionName = config.get("collectionName").toString();


        String appFileName = prepareInputFileName("application", application.getName(),collectionName);

        files = fileTransfer( importUG, appFileName, config, s3Import, 0 );

        FileParser(jobExecution);
    }

    /**
     * Imports a specific applications from an organization
     */
    private void importApplicationFromOrg( UUID organizationUUID, UUID applicationId, final Map<String, Object> config,
                                           final JobExecution jobExecution, S3Import s3Import ) throws Exception {

        //retrieves import entity
        Import importUG = getImportEntity(jobExecution);

        ApplicationInfo application = managementService.getApplicationInfo( applicationId );

        if(application == null) {
            throw new ApplicationNotFoundException("Application Not Found");
        }

        String appFileName = prepareInputFileName("application", application.getName(), null);

        files = fileTransfer( importUG, appFileName, config, s3Import, 1 );

        FileParser(jobExecution);
    }

    /**
     * Imports All Applications from an Organization
     */
    private void importApplicationsFromOrg( UUID organizationUUID, final Map<String, Object> config,
                                            final JobExecution jobExecution, S3Import s3Import ) throws Exception {

        // retrieves import entity
        Import importUG = getImportEntity(jobExecution);
        String appFileName = null;

        OrganizationInfo organizationInfo = managementService.getOrganizationByUuid(organizationUUID);
        if(organizationInfo == null) {
            throw new OrganizationNotFoundException("Organization Not Found");
        }

        appFileName = prepareInputFileName( "organization", organizationInfo.getName() , null );
        files = fileTransfer( importUG, appFileName, config, s3Import, 2 );

        FileParser(jobExecution);
    }

    /**
     * @param type just a label such us: organization, application.
     *
     * @return the file name concatenated with the type and the name of the collection
     */
    protected String prepareInputFileName( String type, String name, String CollectionName ) {
        StringBuilder str = new StringBuilder();
        // in case of type organization --> the file name will be "<org_name>/"
        if(type.equals("organization")) {
            str.append(name);
            str.append("/");
        }
        else if(type.equals("application")) {
            // in case of type application --> the file name will be "<org_name>/<app_name>."
            str.append(name);
            str.append(".");
            if (CollectionName != null) {
                // in case of type application and collection import --> the file name will be "<org_name>/<app_name>.<collection_name>."
                str.append(CollectionName);
                str.append(".");
            }
        }

        String inputFileName = str.toString();

        return inputFileName;
    }

    /**
     *
     * @param importUG Import instance
     * @param appFileName   the base file name for the files to be downloaded
     * @param config    the config information for the import job
     * @param s3Import  s3import instance
     * @param type  it indicates the type of import. 0 - Collection , 1 - Application and 2 - Organization
     * @return
     */
    public ArrayList<File> fileTransfer( Import importUG, String appFileName, Map<String, Object> config,
                                         S3Import s3Import , int type) {
        ArrayList<File> files = new ArrayList<File>();
        try {
            files  =  s3Import.copyFromS3(config, appFileName , type);
        }
        catch ( Exception e ) {
            importUG.setErrorMessage(e.getMessage());
            importUG.setState(Import.State.FAILED);
        }
        return files;
    }

    /**
     * The loops through each temp file and parses it to store the entities from the json back into usergrid
     * @throws Exception
     */
    private void FileParser(JobExecution jobExecution) throws Exception {

        // add properties to the import entity
        Import importUG = getImportEntity(jobExecution);
        EntityManager rootEm = emf.getEntityManager(MANAGEMENT_APPLICATION_ID);



        Map<String,Object> fileMetadata = new HashMap<String, Object>();
        ArrayList<Map<String,Object>> value = new ArrayList<Map<String, Object>>();

        if (!((Map<String,Object>)importUG.getDynamicProperties()).containsKey("files")) {
            // create the structure for file metadata and initialize it
            for (File collectionFile : files) {
                Map<String, Object> singleFile = new HashMap<String, Object>();
                singleFile.put("name", collectionFile.getName());
                singleFile.put("completed", new Boolean(false));
                singleFile.put("lastUpdatedUUID", new String(""));
                value.add(singleFile);
            }

            fileMetadata.put("files", value);
            importUG.addProperties(fileMetadata);
            rootEm.update(importUG);
        }

        ArrayList fileNames = (ArrayList)importUG.getDynamicProperties().get("files");
        int i=0;

        for(File collectionFile : files) {

            Map<String,Object> fileInfo = (Map<String,Object>)fileNames.get(i);
            boolean completed = ((Boolean)fileInfo.get("completed")).booleanValue();
            // on resume, completed files will not be traversed again
            if(!completed) {

                if(!isValidJSON(collectionFile,rootEm,importUG,i)){
                    i++;
                    continue;
                }

                String applicationName = collectionFile.getPath().split("\\.")[0];

                ApplicationInfo application = managementService.getApplicationInfo(applicationName);

                JsonParser jp = getJsonParserForFile(collectionFile);
                String lastUpdatedUUID  = fileInfo.get("lastUpdatedUUID").toString();

                // this handles partially completed files by updating entities from the point of failure
                if(!lastUpdatedUUID.equals(""))
                {
                    // go till the last updated entity
                    while(!jp.getText().equals(lastUpdatedUUID)) {
                        jp.nextToken();
                    }

                    // skip the last one and start from teh next one
                    while(!(jp.getCurrentToken()==JsonToken.END_OBJECT && jp.nextToken() == JsonToken.START_OBJECT)) {
                        jp.nextToken();
                    }
                }

                // get to start of an object i.e next entity.
                while(jp.getCurrentToken() != JsonToken.START_OBJECT) {
                    jp.nextToken();
                }

                EntityManager em = emf.getEntityManager(application.getId());

                //TODO: remove roles and take care of it later when importing applications
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    importEntityStuff(jp, em, rootEm, importUG, i);
                }
                jp.close();
            }
            // mark file as completed
            ((Map<String,Object>)fileNames.get(i)).put("completed",true);
            rootEm.update(importUG);
            i++;
        }
    }

    private boolean isValidJSON( File collectionFile, EntityManager rootEm, Import importUG, int index) throws Exception  {

        ArrayList fileNames = (ArrayList) importUG.getDynamicProperties().get("files");
        boolean valid = false;
        try {
            final JsonParser jp = jsonFactory.createJsonParser(collectionFile);
            while (jp.nextToken() != null) {
            }
            valid = true;
        } catch (JsonParseException e) {
            e.printStackTrace();
            ((Map<String, Object>) fileNames.get(index)).put("Error Message", e.getMessage());
            rootEm.update(importUG);
        } catch (IOException e) {
            e.printStackTrace();
            ((Map<String, Object>) fileNames.get(index)).put("Miscellaneous error", e.getMessage());
            rootEm.update(importUG);
        }
        return valid;
    }


    private JsonParser getJsonParserForFile( File collectionFile ) throws Exception {
        JsonParser jp = jsonFactory.createJsonParser( collectionFile );
        jp.setCodec( new ObjectMapper() );
        return jp;
    }


    /**
     * Imports the entity's connecting references (collections, connections and dictionaries)
     *
     * @param jp JsonPrser pointing to the beginning of the object.
     */
    private void importEntityStuff(final JsonParser jp, final EntityManager em, EntityManager rootEm, Import importUG, int index) throws Exception {

        final JsonParserObservable subscribe = new JsonParserObservable(jp,em,rootEm,importUG, index);

        final Observable<WriteEvent> observable = Observable.create(subscribe);

        /**
         * This is the action we want to perform for every UUID we receive
         */
        final Action1<WriteEvent> doWork = new Action1<WriteEvent>() {
            @Override
            public void call(WriteEvent writeEvent) {
                writeEvent.doWrite(em);
            }
//            @Override
//            public void call(EntityWrapper jsonEntity){
//                try {
//                            em.create(jsonEntity.entityUuid, jsonEntity.entityType, jsonEntity.properties);
//                            em.getRef(jsonEntity.entityUuid);
//
//
//                    System.out.println("Emitting UUID " + jsonEntity.entityUuid + " on thread " + Thread.currentThread().getName() );
//
//                }catch (Exception e) {
//                    System.out.println("something went wrong while creating this - " + e);
//
//                }
//            }

        };


        /**
         * This is boilerplate glue code.  We have to follow this for the parallel operation.  In the "call"
         * method we want to simply return the input observable + the chain of operations we want to invoke
         */
        observable.parallel(new Func1<Observable<WriteEvent>, Observable<WriteEvent>>() {
            @Override
            public Observable< WriteEvent> call(Observable<WriteEvent> entityWrapperObservable) {
                return entityWrapperObservable.doOnNext(doWork);
            }
        }, Schedulers.io() ).toBlocking().last();


    }

    private interface WriteEvent{
        public void doWrite(EntityManager em);
    }

    private final class EntityEvent implements WriteEvent{
        UUID entityUuid;
        String entityType;
        Map<String, Object> properties;
        EntityEvent(UUID entityUuid, String entityType,Map<String, Object> properties){
            this.entityUuid = entityUuid;
            this.entityType = entityType;
            this.properties = properties;
        }
        @Override
        public void doWrite(EntityManager em) {
            try {
                em.create(entityUuid, entityType, properties);
                em.getRef(entityUuid);
                System.out.println("Emitting UUID " + entityUuid + " on thread " + Thread.currentThread().getName() );
                }catch (Exception e) {
                    System.out.println("something went wrong while creating this - " + e);
                }
        }
    }
    private final class ConnectionEvent implements WriteEvent{
        EntityRef ownerEntityRef;
        String connectionType;
        EntityRef entryRef;

        ConnectionEvent(EntityRef ownerEntityRef, String connectionType, EntityRef entryRef){
            this.ownerEntityRef = ownerEntityRef;
            this.connectionType = connectionType;
            this.entryRef = entryRef;

        }

        @Override
        public void doWrite(EntityManager em) {
            try {
                em.createConnection(ownerEntityRef, connectionType, entryRef);
                System.out.println("creating connection " + connectionType + " on thread " + Thread.currentThread().getName() );
            }catch (Exception e) {
                System.out.println("something went wrong while creating this - " + e);
            }
        }
    }
    private final class DictionaryEvent implements WriteEvent{

        EntityRef ownerEntityRef;
        String dictionaryName;
        Map<String, Object> dictionary;

        DictionaryEvent(EntityRef ownerEntityRef, String dictionaryName, Map<String, Object> dictionary){
            this.ownerEntityRef = ownerEntityRef;
            this.dictionaryName = dictionaryName;
            this.dictionary = dictionary;
        }

        @Override
        public void doWrite(EntityManager em) {
            try {
                em.addMapToDictionary(ownerEntityRef, dictionaryName, dictionary);
                System.out.println("creating dictionary  " + dictionaryName + " on thread " + Thread.currentThread().getName() );
            }catch (Exception e) {
                System.out.println("something went wrong while creating this - " + e);
            }
        }
    }



    private final class JsonParserObservable implements Observable.OnSubscribe<WriteEvent> {
        private final JsonParser jp;
        EntityManager em;
        EntityManager rootEm;
        Import importUG;
        int index;

        private int entityCount = 0;

        JsonParserObservable(JsonParser parser, EntityManager em, EntityManager rootEm, Import importUG, int index ) {
            this.jp = parser;
            this.em = em;
            this.rootEm = rootEm;
            this.importUG = importUG;
            this.index = index;
        }

        @Override
        public void call(final Subscriber<? super WriteEvent> subscriber) {
            ArrayList fileNames = (ArrayList) importUG.getDynamicProperties().get("files");

            WriteEvent entityWrapper = null;
            Entity entity = null;
            EntityRef ownerEntityRef = null;
            String entityUuid = "";
            String entityType = "";
            try {
                while (!subscriber.isUnsubscribed() && jp.nextToken() != JsonToken.END_OBJECT) {
                    String collectionName = jp.getCurrentName();
                    try {
                        // create the connections
                        if (collectionName.equals("connections")) {

                            jp.nextToken(); // START_OBJECT
                            while (jp.nextToken() != JsonToken.END_OBJECT) {
                                String connectionType = jp.getCurrentName();

                                jp.nextToken(); // START_ARRAY
                                while (jp.nextToken() != JsonToken.END_ARRAY) {
                                    String entryId = jp.getText();

                                    EntityRef entryRef = new SimpleEntityRef(UUID.fromString(entityUuid));
                                    entityWrapper = new ConnectionEvent(ownerEntityRef, connectionType, entryRef);
                                    subscriber.onNext(entityWrapper);
                                    subscriber.onCompleted();
                                }
                            }
                        }
                        // add dictionaries
                        else if (collectionName.equals("dictionaries")) {

                            jp.nextToken(); // START_OBJECT
                            while (jp.nextToken() != JsonToken.END_OBJECT) {

                                String dictionaryName = jp.getCurrentName();

                                jp.nextToken();

                                @SuppressWarnings("unchecked") Map<String, Object> dictionary = jp.readValueAs(HashMap.class);
                                entityWrapper = new DictionaryEvent(ownerEntityRef, dictionaryName, dictionary);
                                subscriber.onNext(entityWrapper);
                                subscriber.onCompleted();
                            }
                        } else {
                            // Regular collections
                            jp.nextToken(); // START_OBJECT

                            Map<String, Object> properties = new HashMap<String, Object>();

                            JsonToken token = jp.nextToken();

                            while (token != JsonToken.END_OBJECT) {
                                if (token == JsonToken.VALUE_STRING || token == JsonToken.VALUE_NUMBER_INT) {
                                    String key = jp.getCurrentName();
                                    if (key.equals("uuid")) {
                                        entityUuid = jp.getText();

                                    } else if (key.equals("type")) {
                                        entityType = jp.getText();
                                    } else if (key.length() != 0 && jp.getText().length() != 0) {
                                        String value = jp.getText();
                                        properties.put(key, value);
                                    }
                                }
                                token = jp.nextToken();
                            }
                            entityWrapper = new EntityEvent(UUID.fromString(entityUuid), entityType, properties);
                            subscriber.onNext(entityWrapper);
                            ownerEntityRef = new SimpleEntityRef(entityType,UUID.fromString(entityUuid));
                          //  ownerEntityRef = em.getRef(UUID.fromString(entityUuid));
                            subscriber.onCompleted();
                        }
                    } catch (IllegalArgumentException e) {
                        // skip illegal entity UUID and go to next one
                        ((Map<String, Object>) fileNames.get(index)).put("Entity Creation Error", e.getMessage());
                        rootEm.update(importUG);
                        subscriber.onError( e );
                    } catch (Exception e) {
                        // skip illegal entity UUID and go to next one
                        ((Map<String, Object>) fileNames.get(index)).put("Miscellaneous Error", e.getMessage());
                        rootEm.update(importUG);
                        subscriber.onError( e );
                    }
                }

                // update the last updated entity
                if (entity != null) {
                    entityCount++;
                    if (entityCount == 2000) {
                        ((Map<String, Object>) fileNames.get(index)).put("lastUpdatedUUID", entityUuid);
                        rootEm.update(importUG);
                        entityCount = 0;
                    }
                }


            } catch (Exception e) {
                System.out.println("something went wrong in observable json parser - " + e);


            }

        }
    }

}


class OrganizationNotFoundException extends Exception {
    OrganizationNotFoundException(String s) {
        super(s);
    }
}
class ApplicationNotFoundException extends Exception {
    ApplicationNotFoundException(String s) {
        super(s);
    }
}

