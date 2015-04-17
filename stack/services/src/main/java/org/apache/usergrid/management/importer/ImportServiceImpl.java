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

import com.google.common.base.Preconditions;
import org.apache.usergrid.batch.JobExecution;
import org.apache.usergrid.batch.service.SchedulerService;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.entities.FailedImportEntity;
import org.apache.usergrid.persistence.entities.FileImport;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.entities.JobData;
import org.apache.usergrid.persistence.exceptions.EntityNotFoundException;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Query.Level;
import org.apache.usergrid.utils.InflectionUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;


public class ImportServiceImpl implements ImportService {

    public static final String IMPORT_ID = "importId";
    public static final String IMPORT_JOB_NAME = "importJob";
    public static final String FILE_IMPORT_ID = "fileImportId";
    public static final String FILE_IMPORT_JOB_NAME = "fileImportJob";
    public static final int HEARTBEAT_COUNT = 50;

    public static final String APP_IMPORT_CONNECTION ="imports";
    public static final String IMPORT_FILE_INCLUDES_CONNECTION = "files";

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);

    int MAX_FILE_IMPORTS = 1000; // max number of file import jobs / import job

    protected EntityManagerFactory emf;

    private SchedulerService sch;

    private ManagementService managementService;

    private JsonFactory jsonFactory = new JsonFactory();


    @PostConstruct
    public void init(){
    }


    /**
     * This schedules the main import Job.
     *
     * @param config configuration of the job to be scheduled
     * @return it returns the UUID of the scheduled job
     */
    @Override
    public Import schedule(final UUID application,  Map<String, Object> config ) throws Exception {

        Preconditions.checkNotNull(config, "import information cannot be null");
        Preconditions.checkNotNull( application, "application cannot be null" );

        final EntityManager rootEM;
        try {
            rootEM = emf.getEntityManager(emf.getManagementAppId());
        } catch (Exception e) {
            logger.error("application doesn't exist within the current context");
            return null;
        }

        Import importEntity = new Import();
        importEntity.setState(Import.State.CREATED);

        // create the import entity to store all metadata about the import job
        try {
            importEntity = rootEM.create( importEntity );
        } catch (Exception e) {
            logger.error("Import entity creation failed");
            return null;
        }

        // update state for import job to created

        // set data to be transferred to importInfo
        JobData jobData = new JobData();
        jobData.setProperty("importInfo", config);
        jobData.setProperty(IMPORT_ID, importEntity.getUuid());

        long soonestPossible = System.currentTimeMillis() + 250; //sch grace period

        // schedule import job
        sch.createJob(IMPORT_JOB_NAME, soonestPossible, jobData);

        // update state for import job to created
        importEntity.setState(Import.State.SCHEDULED);
        rootEM.update(importEntity);

        final EntityRef appInfo = getApplicationInfoEntity(rootEM, application);

        //now link it to the application
        rootEM.createConnection(appInfo, APP_IMPORT_CONNECTION, importEntity);

        return importEntity;
    }


    @Override
    public Results getImports( final UUID applicationId, @Nullable  final String ql,  @Nullable final String cursor ) {
        Preconditions.checkNotNull( applicationId, "applicationId must be specified" );

        try {
            final EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );
            final Entity appInfo = getApplicationInfoEntity(rootEm, applicationId);

            Query query = Query.fromQLNullSafe( ql );
            query.setCursor( cursor );

            //set our entity type
            query.setEntityType( Schema.getDefaultSchema().getEntityType( Import.class ) );

            return rootEm.searchCollection( appInfo, APP_IMPORT_CONNECTION, query );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to get import entity", e );
        }
    }


    @Override
    public Import getImport( final UUID applicationId, final UUID importId ) {
        Preconditions.checkNotNull( applicationId, "applicationId must be specified" );
        Preconditions.checkNotNull( importId, "importId must be specified" );

        try {
            final EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );

            final Entity appInfo = getApplicationInfoEntity(rootEm, applicationId);
            final Import importEntity = rootEm.get( importId, Import.class );

            // check if it's on the path
            if ( !rootEm.isConnectionMember( appInfo, APP_IMPORT_CONNECTION, importEntity ) ) {
                return null;
            }

            return importEntity;
        }catch(Exception e){
            throw new RuntimeException("Unable to get import entity", e );
        }

    }


    private Entity getApplicationInfoEntity(final EntityManager rootEm, final UUID applicationId) throws Exception {
        final Entity entity = rootEm.get( new SimpleEntityRef( CpNamingUtils.APPLICATION_INFO, applicationId ) );

        if(entity == null){
            throw new EntityNotFoundException( "Cound not find application with id "  + applicationId);
        }

        return entity;
    }

    @Override
    public Results getFileImports(final UUID applicationId, final UUID importId,
                                  @Nullable  final String ql, @Nullable final String cursor ) {

        Preconditions.checkNotNull( applicationId, "applicationId must be specified" );
               Preconditions.checkNotNull( importId, "importId must be specified" );

        try {
            final EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );


            final Import importEntity = getImport( applicationId, importId );

            Query query = Query.fromQLNullSafe( ql );
            query.setCursor( cursor );
            query.setConnectionType( IMPORT_FILE_INCLUDES_CONNECTION );
            query.setResultsLevel( Level.ALL_PROPERTIES );


            //set our entity type
            query.setEntityType( Schema.getDefaultSchema().getEntityType( FileImport.class ) );

            return rootEm.searchConnectedEntities( importEntity, query );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to get import entity", e );
        }

    }


    @Override
    public FileImport getFileImport(final UUID applicationId,  final UUID importId, final UUID fileImportId ) {
        try {
            final EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );

            final Import importEntity = getImport( applicationId, importId );

            if ( importEntity == null ) {
                throw new EntityNotFoundException( "Import not found with id " + importId );
            }

            final FileImport fileImport = rootEm.get( importId, FileImport.class );


            // check if it's on the path
            if ( !rootEm.isConnectionMember( importEntity, APP_IMPORT_CONNECTION, fileImport ) ) {
                return null;
            }

            return fileImport;
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to load file import", e );
        }
    }


    @Override
    public Results getFailedImportEntities(final UUID applicationId,  final UUID importId, final UUID fileImportId,
                                           @Nullable  final String ql, @Nullable final String cursor ) {

        Preconditions.checkNotNull( applicationId, "applicationId must be specified" );
        Preconditions.checkNotNull( importId, "importId must be specified" );
        Preconditions.checkNotNull( fileImportId, "fileImportId must be specified" );

        try {
            final EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );


            final FileImport importEntity = getFileImport(applicationId, importId, fileImportId);

            Query query = Query.fromQLNullSafe( ql );
            query.setCursor( cursor );
            query.setConnectionType( FileImportTracker.ERRORS_CONNECTION_NAME );
            query.setResultsLevel( Level.ALL_PROPERTIES );


            //set our entity type
            query.setEntityType( Schema.getDefaultSchema().getEntityType( FailedImportEntity.class ) );

            return rootEm.searchConnectedEntities( importEntity,  query );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to get import entity", e );
        }
    }


    @Override
    public FailedImportEntity getFailedImportEntity(final UUID applicationId, final UUID importId,
                                                    final UUID fileImportId, final UUID failedImportId ) {
        try {
            final EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );


            final FileImport importEntity = getFileImport( applicationId, importId, fileImportId );

            if ( importEntity == null ) {
                throw new EntityNotFoundException( "Import not found with id " + importId );
            }


            final FailedImportEntity fileImport = rootEm.get( importId, FailedImportEntity.class );


            // check if it's on the path
            if ( !rootEm.isConnectionMember( importEntity, FileImportTracker.ERRORS_CONNECTION_NAME, fileImport ) ) {
                return null;
            }

            return fileImport;
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to load file import", e );
        }
    }


    /**
     * This schedules the sub  FileImport Job
     *
     * @param file file to be scheduled
     * @return it returns the UUID of the scheduled job
     * @throws Exception
     */
    private JobData createFileTask( Map<String, Object> config, String file, EntityRef importRef ) throws Exception {

        logger.debug("scheduleFile() for import {}:{} file {}",
            new Object[]{importRef.getType(), importRef.getType(), file});

        EntityManager rootEM;

        try {
            rootEM = emf.getEntityManager(emf.getManagementAppId());
        } catch (Exception e) {
            logger.error("application doesn't exist within the current context");
            return null;
        }

        // create a FileImport entity to store metadata about the fileImport job
        UUID applicationId = (UUID)config.get("applicationId");
        FileImport fileImport = new FileImport( file, applicationId );
        fileImport = rootEM.create(fileImport);

        Import importEntity = rootEM.get(importRef, Import.class);

        try {
            // create a connection between the main import job and the sub FileImport Job
            rootEM.createConnection(importEntity, IMPORT_FILE_INCLUDES_CONNECTION, fileImport);

            logger.debug("Created connection from {}:{} to {}:{}",
                new Object[] {
                    importEntity.getType(), importEntity.getUuid(),
                    fileImport.getType(), fileImport.getUuid()
                });

        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }

        // mark the File Import Job as created
        fileImport.setState(FileImport.State.CREATED);
        rootEM.update( fileImport );

        // set data to be transferred to the FileImport Job
        JobData jobData = new JobData();
        jobData.setProperty("File", file);
        jobData.setProperty(FILE_IMPORT_ID, fileImport.getUuid());
        jobData.addProperties(config);

        // update state of the job to Scheduled
        fileImport.setState(FileImport.State.SCHEDULED);
        rootEM.update(fileImport);

        return jobData;
    }


    private int getConnectionCount( final Import importRoot ) {

        try {

            EntityManager rootEM = emf.getEntityManager( emf.getManagementAppId() );
            Query query = Query.fromQL( "select *" );
            query.setEntityType("file_import");
            query.setConnectionType( IMPORT_FILE_INCLUDES_CONNECTION );
            query.setLimit(MAX_FILE_IMPORTS);

            // TODO, this won't work with more than 100 files
            Results entities = rootEM.searchConnectedEntities( importRoot, query );
            return entities.size();

            // see ImportConnectsTest()
//            Results entities = rootEM.getConnectedEntities(
//              importRoot, "includes", null, Level.ALL_PROPERTIES );
//            PagingResultsIterator itr = new PagingResultsIterator( entities );
//            int count = 0;
//            while ( itr.hasNext() ) {
//                itr.next();
//                count++;
//            }
//            return count;
        }
        catch ( Exception e ) {
            logger.error( "application doesn't exist within the current context" );
            throw new RuntimeException( e );
        }
    }


    /**
     * Schedule the file tasks.  This must happen in 2 phases.  The first is linking the
     * sub files to the master the second is scheduling them to run.
     */
    private JobData scheduleFileTasks( final JobData jobData ) {

        long soonestPossible = System.currentTimeMillis() + 250; //sch grace period

        // schedule file import job
        return sch.createJob(FILE_IMPORT_JOB_NAME, soonestPossible, jobData);
    }


    /**
     * Query Entity Manager for the state of the Import Entity. This corresponds to the GET /import
     */
    @Override
    public Import.State getState( UUID uuid ) throws Exception {

        Preconditions.checkNotNull( uuid, "uuid cannot be null" );

        EntityManager rootEm = emf.getEntityManager(emf.getManagementAppId());

        //retrieve the import entity.
        Import importUG = rootEm.get(uuid, Import.class);

        if (importUG == null) {
            throw new EntityNotFoundException( "Could not find entity with uuid " + uuid );
        }

        return importUG.getState();
    }


    /**
     * Query Entity Manager for the error message generated for an import job.
     */
    @Override
    public String getErrorMessage(final UUID uuid) throws Exception {

        //get application entity manager

        if (uuid == null) {
            logger.error("getErrorMessage(): UUID passed in cannot be null.");
            return "UUID passed in cannot be null";
        }

        EntityManager rootEm = emf.getEntityManager(emf.getManagementAppId());

        //retrieve the import entity.
        Import importUG = rootEm.get(uuid, Import.class);

        if (importUG == null) {
            logger.error("getErrorMessage(): no entity with that uuid was found");
            return "No Such Element found";
        }
        return importUG.getErrorMessage();
    }


    /**
     * Returns the Import Entity that stores all meta-data for the particular import Job
     *
     * @param jobExecution the import job details
     * @return Import Entity
     */
    @Override
    public Import getImportEntity(final JobExecution jobExecution) throws Exception {

        UUID importId = (UUID) jobExecution.getJobData().getProperty(IMPORT_ID);
        EntityManager importManager = emf.getEntityManager(emf.getManagementAppId());

        return importManager.get(importId, Import.class);
    }


    /**
     * Returns the File Import Entity that stores all meta-data for the particular sub File import Job
     * @return File Import Entity
     */
    @Override
    public FileImport getFileImportEntity(final JobExecution jobExecution) throws Exception {
        UUID fileImportId = (UUID) jobExecution.getJobData().getProperty(FILE_IMPORT_ID);
        EntityManager em = emf.getEntityManager(emf.getManagementAppId());
        return em.get(fileImportId, FileImport.class);
    }


    public EntityManagerFactory getEmf() {
        return emf;
    }


    public void setEmf(final EntityManagerFactory emf) {
        this.emf = emf;
    }


    public ManagementService getManagementService() {

        return managementService;
    }


    public void setManagementService(final ManagementService managementService) {
        this.managementService = managementService;
    }


    public void setSch(final SchedulerService sch) {
        this.sch = sch;
    }


    /**
     * This method creates sub-jobs for each file i.e. File Import Jobs.
     *
     * @param jobExecution the job created by the scheduler with all the required config data
     */
    @Override
    public void doImport(JobExecution jobExecution) throws Exception {

        logger.debug("doImport()");

        Map<String, Object> config =
            (Map<String, Object>) jobExecution.getJobData().getProperty("importInfo");
        if (config == null) {
            logger.error("doImport(): Import Information passed through is null");
            return;
        }

        Map<String, Object> properties =
            (Map<String, Object>)config.get("properties");
        Map<String, Object> storage_info =
            (Map<String, Object>) properties.get("storage_info");

        String bucketName = (String) storage_info.get("bucket_location");
        String accessId = (String) storage_info.get( "s3_access_id" );
        String secretKey = (String) storage_info.get( "s3_key" );

        // get Import Entity from the management app, update it to show that job has started

        final EntityManager rootEM = emf.getEntityManager(emf.getManagementAppId());
        UUID importId = (UUID) jobExecution.getJobData().getProperty(IMPORT_ID);
        Import importEntity = rootEM.get(importId, Import.class);

        importEntity.setState(Import.State.STARTED);
        importEntity.setStarted(System.currentTimeMillis());
        importEntity.setErrorMessage(" ");
        rootEM.update(importEntity);
        logger.debug("doImport(): updated state");

        // if no S3 importer was passed in then create one

        S3Import s3Import;
        Object s3PlaceHolder = jobExecution.getJobData().getProperty("s3Import");
        try {
            if (s3PlaceHolder != null) {
                s3Import = (S3Import) s3PlaceHolder;
            } else {
                s3Import = new S3ImportImpl();
            }
        } catch (Exception e) {
            logger.error("doImport(): Error creating S3Import", e);
            importEntity.setErrorMessage(e.getMessage());
            importEntity.setState(Import.State.FAILED);
            rootEM.update(importEntity);
            return;
        }

        // get list of all JSON files in S3 bucket

        final List<String> bucketFiles;
        try {

            if (config.get("organizationId") == null) {
                logger.error("doImport(): No organization could be found");
                importEntity.setErrorMessage("No organization could be found");
                importEntity.setState(Import.State.FAILED);
                rootEM.update(importEntity);
                return;

            } else {

                if (config.get("applicationId") == null) {
                    throw new UnsupportedOperationException("Import applications not supported");

                }  else {
                    bucketFiles = s3Import.getBucketFileNames( bucketName, ".json", accessId, secretKey );
                }
            }

        } catch (OrganizationNotFoundException | ApplicationNotFoundException e) {
            importEntity.setErrorMessage(e.getMessage());
            importEntity.setState(Import.State.FAILED);
            rootEM.update(importEntity);
            return;
        }


        // schedule a FileImport job for each file found in the bucket

        if ( bucketFiles.isEmpty() )  {
            importEntity.setState(Import.State.FINISHED);
            importEntity.setErrorMessage("No files found in the bucket: " + bucketName);
            rootEM.update(importEntity);

        } else {

            Map<String, Object> fileMetadata = new HashMap<>();
            ArrayList<Map<String, Object>> value = new ArrayList<>();
            final List<JobData> fileJobs = new ArrayList<>(bucketFiles.size());

            // create the Entity Connection and set up metadata for each job

            for ( String bucketFile : bucketFiles ) {
                final JobData jobData = createFileTask(config, bucketFile, importEntity);
                fileJobs.add( jobData) ;
            }

            int retries = 0;
            int maxRetries = 60;
            boolean done = false;
            while ( !done && retries++ < maxRetries ) {

                final int count = getConnectionCount(importEntity);
                if ( count == fileJobs.size() ) {
                    logger.debug("Got ALL {} of {} expected connections", count, fileJobs.size());
                    done = true;
                } else {
                    logger.debug("Got {} of {} expected connections. Waiting...", count, fileJobs.size());
                    Thread.sleep(1000);
                }
            }
            if ( retries >= maxRetries ) {
                throw new RuntimeException("Max retries was reached");
            }

            // schedule each job

            for ( JobData jobData: fileJobs ) {

                final JobData scheduled = scheduleFileTasks( jobData );

                Map<String, Object> fileJobID = new HashMap<>();
                    fileJobID.put("FileName", scheduled.getProperty( "File" ));
                    fileJobID.put("JobID", scheduled.getUuid());
                value.add(fileJobID);
            }

            fileMetadata.put("files", value);
            importEntity.addProperties(fileMetadata);
            importEntity.setFileCount(fileJobs.size());
            rootEM.update(importEntity);
        }
    }


    @Override
    public void downloadAndImportFile(JobExecution jobExecution) {

        // get values we need

        Map<String, Object> properties =
            (Map<String, Object>)jobExecution.getJobData().getProperty("properties");
        if (properties == null) {
            logger.error("downloadAndImportFile(): Import Information passed through is null");
            return;
        }
        Map<String, Object> storage_info =
            (Map<String, Object>) properties.get("storage_info");

        String bucketName = (String) storage_info.get("bucket_location");
        String accessId = (String) storage_info.get( "s3_access_id");
        String secretKey = (String) storage_info.get( "s3_key" );

        EntityManager rootEM = emf.getEntityManager( emf.getManagementAppId() );

       // get the file import entity

        FileImport fileImport;
        try {
            fileImport = getFileImportEntity(jobExecution);
        } catch (Exception e) {
            logger.error("Error updating fileImport to set state of file import", e);
            return;
        }

        // tracker flushes every 100 entities
        final FileImportTracker tracker = new FileImportTracker( emf, fileImport, 100 );

        String fileName = jobExecution.getJobData().getProperty("File").toString();
        UUID targetAppId = (UUID) jobExecution.getJobData().getProperty("applicationId");

        // is job already done?
        if (   FileImport.State.FAILED.equals( fileImport.getState() )
            || FileImport.State.FINISHED .equals(fileImport.getState()) ) {
            return;
        }

        // update FileImport Entity to indicate that we have started

        logger.debug("downloadAndImportFile() for file {} ", fileName);
        try {
            rootEM.update( fileImport );
            fileImport.setState(FileImport.State.STARTED);
            rootEM.update(fileImport);

            if ( rootEM.get( targetAppId ) == null ) {
                tracker.fatal("Application " + targetAppId + " does not exist");
                return;
            }

        } catch (Exception e) {
            tracker.fatal("Application " + targetAppId + " does not exist");
            checkIfComplete( rootEM, fileImport );
            return;
        }

        EntityManager targetEm = emf.getEntityManager( targetAppId );

        // download file from S3, if no S3 importer was passed in then create one

        File downloadedFile = null;
        S3Import s3Import;
        Object s3PlaceHolder = jobExecution.getJobData().getProperty("s3Import");
        try {
            if (s3PlaceHolder != null) {
                s3Import = (S3Import) s3PlaceHolder;
            } else {
                s3Import = new S3ImportImpl();
            }
        } catch (Exception e) {
            tracker.fatal("Error connecting to S3: " + e.getMessage());
            checkIfComplete( rootEM, fileImport );
            return;
        }

        try {
            downloadedFile = s3Import.copyFileFromBucket(
                fileName, bucketName, accessId, secretKey );
        } catch (Exception e) {
            tracker.fatal("Error downloading file: " +  e.getMessage());
            checkIfComplete( rootEM, fileImport );
            return;
        }

        // parse JSON data, create Entities and Connections from import data

        try {
            parseEntitiesAndConnectionsFromJson(
                jobExecution, downloadedFile, targetEm, rootEM, fileImport, tracker);

        } catch (Exception e) {
            tracker.fatal(e.getMessage());
        }

        checkIfComplete( rootEM, fileImport );
    }


    private Import getImportEntity( final EntityManager rootEm, final FileImport fileImport ) {
        try {
            Results importJobResults =
                rootEm.getConnectingEntities( fileImport, IMPORT_FILE_INCLUDES_CONNECTION,
                    null, Level.ALL_PROPERTIES );

            List<Entity> importEntities = importJobResults.getEntities();
            final Import importEntity = ( Import ) importEntities.get( 0 ).toTypedEntity();
            return importEntity;
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to import entity" );
        }
    }

    /**
     * Check if we're the last job on failure
     */
    private void checkIfComplete( final EntityManager rootEM, final FileImport fileImport ) {
        int failCount = 0;
        int successCount = 0;

        final Import importEntity = getImportEntity( rootEM, fileImport );

        try {

            // wait for query index to catch up

            // TODO: better way to wait for indexes to catch up
            try { Thread.sleep(5000); } catch ( Exception intentionallyIgnored ) {}

            // get file import entities for this import job

            Query query = new Query();
            query.setEntityType( Schema.getDefaultSchema().getEntityType( FileImport.class ) );
            query.setConnectionType( IMPORT_FILE_INCLUDES_CONNECTION );
            query.setLimit( MAX_FILE_IMPORTS );

            Results entities = rootEM.searchConnectedEntities( importEntity, query );
            PagingResultsIterator itr = new PagingResultsIterator( entities );

            if ( !itr.hasNext() ) {
                logger.warn("Found no FileImport entities for import {}, " +
                    "unable to check if complete", importEntity.getUuid());
                return;
            }

            logger.debug( "Checking {} file import jobs to see if we are done for file {}",
                new Object[] { entities.size(), fileImport.getFileName() } );

            // loop through entities, count different types of status

            while ( itr.hasNext() ) {
                FileImport fi = ( FileImport ) itr.next();
                switch ( fi.getState() ) {
                    case FAILED:     // failed, but we may not be complete so continue checking
                        failCount++;
                        break;
                    case FINISHED:   // finished, we can continue checking
                        successCount++;
                        continue;
                    default:         // not something we recognize as complete, short circuit
                        logger.debug( "not done yet, bail out..." ); return;
                }
            }
        }
        catch ( Exception e ) {
            failCount++;
            if ( importEntity != null ) {
                importEntity.setErrorMessage( "Error determining status of file import jobs" );
            }
            logger.debug( "Error determining status of file import jobs", e );
        }

        logger.debug( "successCount = {} failCount = {}", new Object[] { successCount, failCount } );

        if ( importEntity != null ) {
            logger.debug( "FINISHED" );

            if ( failCount == 0 ) {
                importEntity.setState( Import.State.FINISHED );
            }
            else {
                // we had failures, set it to failed
                importEntity.setState( Import.State.FAILED );
            }

            try {
                rootEM.update( importEntity );
                logger.debug("Updated import entity {}:{} with state {}",
                    new Object[] { importEntity.getType(), importEntity.getUuid(), importEntity.getState() } );
            }
            catch ( Exception e ) {
                logger.error( "Error updating import entity", e );
            }
        }


    }


    /**
     * Gets the JSON parser for given file
     *
     * @param collectionFile the file for which JSON parser is required
     */
    private JsonParser getJsonParserForFile(File collectionFile) throws Exception {
        JsonParser jp = jsonFactory.createJsonParser(collectionFile);
        jp.setCodec(new ObjectMapper());
        return jp;
    }


    /**
     * Imports the entity's connecting references (collections, connections and dictionaries)
     *
     * @param execution     The job jobExecution currently running
     * @param file         The file to be imported
     * @param em           Entity Manager for the application being imported
     * @param rootEm       Entity manager for the root applicaition
     * @param fileImport   The file import entity
     */
    private void parseEntitiesAndConnectionsFromJson(
        final JobExecution execution,
        final File file,
        final EntityManager em,
        final EntityManager rootEm,
        final FileImport fileImport,
        final FileImportTracker tracker) throws Exception {


        // tracker flushes every 100 entities
        //final FileImportTracker tracker = new FileImportTracker( emf, fileImport, 100 );

        // function to execute for each write event
        final Action1<WriteEvent> doWork = new Action1<WriteEvent>() {
            @Override
            public void call( WriteEvent writeEvent ) {
                writeEvent.doWrite( em, fileImport, tracker );
            }
        };

        // invokes the heartbeat every HEARTBEAT_COUNT operations
        final Func2<Integer, WriteEvent, Integer> heartbeatReducer = new Func2<Integer, WriteEvent, Integer>() {
            @Override
            public Integer call( final Integer integer, final WriteEvent writeEvent ) {
                final int next = integer.intValue() + 1;
                if ( next % HEARTBEAT_COUNT == 0 ) {
                    execution.heartbeat();
                }
                return next;
            }
        };


        // FIRST PASS: import all entities in the file


        boolean entitiesOnly = true;

        // observable that parses JSON and emits write events
        JsonParser jp = getJsonParserForFile(file);

        // TODO: move JSON parser into observable creation so open/close happens within the stream
        final JsonEntityParserObservable jsonObservableEntities =
            new JsonEntityParserObservable(jp, em, rootEm, fileImport, tracker, entitiesOnly);

        final Observable<WriteEvent> entityEventObservable = Observable.create(jsonObservableEntities);

        // only take while our stats tell us we should continue processing
        // potentially skip the first n if this is a resume operation
        final int entityNumSkip = (int)tracker.getTotalEntityCount();


        entityEventObservable.takeWhile( writeEvent -> !tracker.shouldStopProcessingEntities() ).skip( entityNumSkip )
            .flatMap( writeEvent -> {
                return Observable.just( writeEvent ).doOnNext( doWork );
            }, 10 ).reduce( 0, heartbeatReducer ).toBlocking().last();


        jp.close();

        if ( FileImport.State.FAILED.equals( fileImport.getState() ) ) {
            logger.debug("\n\nFailed to completely write entities, skipping second phase. File: {}\n",
                fileImport.getFileName());
            return;
        }
        logger.debug("\n\nWrote entities. File: {}\n", fileImport.getFileName() );


        // SECOND PASS: import all connections and dictionaries


        entitiesOnly = false;

        // observable that parses JSON and emits write events
        jp = getJsonParserForFile(file);

        // TODO: move JSON parser into observable creation so open/close happens within the stream
        final JsonEntityParserObservable jsonObservableOther =
            new JsonEntityParserObservable(jp, em, rootEm, fileImport, tracker, entitiesOnly);

        final Observable<WriteEvent> otherEventObservable = Observable.create(jsonObservableOther);

        // only take while our stats tell us we should continue processing
        // potentially skip the first n if this is a resume operation
        final int connectionNumSkip = (int)tracker.getTotalConnectionCount();

        // with this code we get asynchronous behavior and testImportWithMultipleFiles will fail
        final int connectionCount = otherEventObservable.takeWhile(
            writeEvent -> !tracker.shouldStopProcessingConnections() ).skip(connectionNumSkip).flatMap( entityWrapper ->{
                return Observable.just(entityWrapper).doOnNext( doWork ).subscribeOn( Schedulers.io() );

        }, 10 ).reduce(0, heartbeatReducer).toBlocking().last();

        jp.close();

        logger.debug("\n\nparseEntitiesAndConnectionsFromJson(): Wrote others for file {}\n",
            fileImport.getFileName());

        if ( FileImport.State.FAILED.equals( fileImport.getState() ) ) {
            logger.debug("\n\nparseEntitiesAndConnectionsFromJson(): failed to completely write entities\n");
            return;
        }

        // flush the job statistics
        tracker.complete();

        if ( FileImport.State.FAILED.equals( fileImport.getState() ) ) {
            logger.debug("\n\nFailed to completely wrote connections and dictionaries. File: {}\n",
                fileImport.getFileName());
            return;
        }
        logger.debug("\n\nWrote connections and dictionaries. File: {}\n", fileImport.getFileName());
    }


    private interface WriteEvent {
        public void doWrite(EntityManager em, FileImport fileImport, FileImportTracker tracker);
    }


    private final class EntityEvent implements WriteEvent {
        UUID entityUuid;
        String entityType;
        Map<String, Object> properties;

        EntityEvent(UUID entityUuid, String entityType, Map<String, Object> properties) {
            this.entityUuid = entityUuid;
            this.entityType = entityType;
            this.properties = properties;
        }



        // Creates entities
        @Override
        public void doWrite(EntityManager em, FileImport fileImport, FileImportTracker tracker) {
            try {
                logger.debug("Writing imported entity {}:{} into app {}",
                    new Object[]{entityType, entityUuid, em.getApplication().getUuid()});

                em.create(entityUuid, entityType, properties);

                tracker.entityWritten();

            } catch (Exception e) {
                logger.error("Error writing entity. From file:" + fileImport.getFileName(), e);

                tracker.entityFailed( e.getMessage() + " From file: " + fileImport.getFileName() );
            }
        }
    }


    private final class ConnectionEvent implements WriteEvent {
        EntityRef ownerEntityRef;
        String connectionType;
        EntityRef entityRef;

        ConnectionEvent(EntityRef ownerEntityRef, String connectionType, EntityRef entryRef) {
            this.ownerEntityRef = ownerEntityRef;
            this.connectionType = connectionType;
            this.entityRef = entryRef;
        }

        // creates connections between entities
        @Override
        public void doWrite(EntityManager em, FileImport fileImport, FileImportTracker tracker) {

            try {
                // TODO: do we need to ensure that all Entity events happen first?
                // TODO: what happens if ConnectionEvents  happen before all entities are saved?

                // Connections are specified as UUIDs with no type
                if (entityRef.getType() == null) {
                    entityRef = em.get(ownerEntityRef.getUuid());
                }

                logger.debug("Creating connection from {}:{} to {}:{}",
                    new Object[]{
                        ownerEntityRef.getType(), ownerEntityRef.getUuid(),
                        entityRef.getType(), entityRef.getUuid()});

                em.createConnection(ownerEntityRef, connectionType, entityRef);

                tracker.connectionWritten();

            } catch (Exception e) {
                logger.error("Error writing connection. From file: " + fileImport.getFileName(), e);

                tracker.connectionFailed( e.getMessage() + " From file: " + fileImport.getFileName() );
            }
        }
    }


    private final class DictionaryEvent implements WriteEvent {

        EntityRef ownerEntityRef;
        String dictionaryName;
        Map<String, Object> dictionary;

        DictionaryEvent(EntityRef ownerEntityRef, String dictionaryName, Map<String, Object> dictionary) {
            this.ownerEntityRef = ownerEntityRef;
            this.dictionaryName = dictionaryName;
            this.dictionary = dictionary;
        }

        // adds map to the dictionary
        @Override
        public void doWrite(EntityManager em, FileImport fileImport, FileImportTracker stats) {
            try {

                logger.debug("Adding map to {}:{} dictionary {}",
                    new Object[]{ownerEntityRef.getType(), ownerEntityRef.getType(), dictionaryName});

                em.addMapToDictionary(ownerEntityRef, dictionaryName, dictionary);

            } catch (Exception e) {
                logger.error("Error writing dictionary. From file: " + fileImport.getFileName(), e);

                // TODO add statistics for dictionary writes and failures
            }
        }
    }


    private final class JsonEntityParserObservable implements Observable.OnSubscribe<WriteEvent> {
        public static final String COLLECTION_OBJECT_NAME = "collections";
        private final JsonParser jp;
        EntityManager em;
        EntityManager rootEm;
        FileImport fileImport;
        FileImportTracker tracker;
        boolean entitiesOnly;


        JsonEntityParserObservable(
            JsonParser parser,
            EntityManager em,
            EntityManager rootEm,
            FileImport fileImport,
            FileImportTracker tracker,
            boolean entitiesOnly) {

            this.jp = parser;
            this.em = em;
            this.rootEm = rootEm;
            this.fileImport = fileImport;
            this.tracker = tracker;
            this.entitiesOnly = entitiesOnly;
        }


       @Override
        public void call(final Subscriber<? super WriteEvent> subscriber) {
            process(subscriber);
        }


        private void process(final Subscriber<? super WriteEvent> subscriber) {

            try {

                // we ignore imported entity type information, entities get the type of the collection
                Stack<JsonToken> objectStartStack = new Stack();
                Stack<String> objectNameStack = new Stack();
                EntityRef lastEntity = null;

                String entityType = null;

                while ( true ) {

                    JsonToken token = jp.nextToken();

                    // nothing left to do.
                    if ( token == null ) {
                        break;
                    }

                    String name = jp.getCurrentName();


                    // start of an object with a field name

                    if ( token.equals( JsonToken.START_OBJECT ) ) {

                        objectStartStack.push( token );

                        // nothing to do
                        if ( name == null ) {
                            continue;
                        }


                        if ( "Metadata".equals( name ) ) {

                            Map<String, Object> entityMap = jp.readValueAs( HashMap.class );

                            UUID uuid = null;
                            if ( entityMap.get( "uuid" ) != null ) {
                                uuid = UUID.fromString((String) entityMap.get("uuid"));
                                lastEntity = new SimpleEntityRef(entityType, uuid);
                            }

                            if ( entitiesOnly ) {
                                //logger.debug("{}Got entity with uuid {}", indent, lastEntity);

                                WriteEvent event = new EntityEvent(uuid, entityType, entityMap);
                                processWriteEvent( subscriber, event);
                            }
                            objectStartStack.pop();
                        }
                        else if ( "connections".equals(name) ) {

                            Map<String, Object> connectionMap = jp.readValueAs( HashMap.class );

                            for ( String type : connectionMap.keySet() ) {
                                List targets = ( List ) connectionMap.get( type );

                                for ( Object targetObject : targets ) {
                                    UUID target = UUID.fromString( ( String ) targetObject );

                                    if ( !entitiesOnly ) {
                                        //logger.debug("{}Got connection {} to {}",
                                            //new Object[]{indent, type, target.toString()});

                                        EntityRef entryRef = new SimpleEntityRef(target);
                                        WriteEvent event = new ConnectionEvent(lastEntity, type, entryRef);
                                        processWriteEvent(subscriber, event);
                                    }
                                }
                            }

                            objectStartStack.pop();

                        } else if ( "dictionaries".equals(name) ) {

                            Map<String, Object> dictionariesMap = jp.readValueAs( HashMap.class );
                            for ( String dname : dictionariesMap.keySet() ) {
                                Map dmap = ( Map ) dictionariesMap.get( dname );

                                if ( !entitiesOnly ) {
                                    //logger.debug("{}Got dictionary {} size {}",
                                        //new Object[] {indent, dname, dmap.size() });

                                    WriteEvent event = new DictionaryEvent(lastEntity, dname, dmap);
                                    processWriteEvent(subscriber, event);
                                }
                            }

                            objectStartStack.pop();

                        } else {
                            // push onto object names we don't immediately understand.  Used for parent detection
                            objectNameStack.push( name );
                        }

                    }  else if (token.equals( JsonToken.START_ARRAY )) {
                         if ( objectNameStack.size() == 1
                                && COLLECTION_OBJECT_NAME.equals( objectNameStack.peek() )) {
                            entityType = InflectionUtils.singularize( name );
                         }

                    } else if ( token.equals( JsonToken.END_OBJECT ) ) {
                        objectStartStack.pop();
                    }
                }

                if ( subscriber != null ) {
                    subscriber.onCompleted();
                }

                logger.debug("process(): done parsing JSON");

            } catch (Exception e) {

                tracker.fatal( e.getMessage() );

                if ( subscriber != null ) {

                    // don't need to blow up here, we handled the problem
                    // if we blow up we may prevent in-flight entities from being written
                    // subscriber.onError(e);

                    // but we are done reading entities
                    subscriber.onCompleted();
                }
            }
        }

        private void processWriteEvent( final Subscriber<? super WriteEvent> subscriber, WriteEvent writeEvent ) {

            if ( subscriber == null ) {

                // this logic makes it easy to remove Rx for debugging purposes
                // no Rx, just do it
                writeEvent.doWrite(em, fileImport, tracker);

            } else {
                subscriber.onNext( writeEvent );
            }
        }

    }
}


/**
 * Custom Exception class for Organization Not Found
 */
class OrganizationNotFoundException extends Exception {
    OrganizationNotFoundException(String s) {
        super(s);
    }
}


/**
 * Custom Exception class for Application Not Found
 */
class ApplicationNotFoundException extends Exception {
    ApplicationNotFoundException(String s) {
        super(s);
    }
}
