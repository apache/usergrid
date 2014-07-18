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
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.entities.Import;
import org.apache.usergrid.persistence.entities.JobData;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;

/**
 * Created by ApigeeCorporation on 7/8/14.
 */
public class ImportServiceImpl implements ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportServiceImpl.class);
    public static final String IMPORT_ID = "importId";
    public static final String IMPORT_JOB_NAME = "importJob";
    private static ArrayList<File> files;

    //dependency injection
    private SchedulerService sch;

    //injected the Entity Manager Factory
    protected EntityManagerFactory emf;

    //inject Management Service to access Organization Data
    private ManagementService managementService;

    //Amount of time that has passed before sending another heart beat in millis
    public static final int TIMESTAMP_DELTA = 5000;

    private JsonFactory jsonFactory = new JsonFactory();

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

        //retrieve the import entity.
        Import importUG = rootEm.get( uuid, Import.class );

        if ( importUG == null ) {
            logger.error( "no entity with that uuid was found" );
            return "No Such Element found";
        }
        return importUG.getState().toString();
    }


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

        Map<String, Object> config = ( Map<String, Object> ) jobExecution.getJobData().getProperty( "importInfo" );
        Object s3PlaceHolder = jobExecution.getJobData().getProperty( "s3Import" );
        S3Import s3Import = null;

        if ( config == null ) {
            logger.error( "Import Information passed through is null" );
            return;
        }

        //get the entity manager for the application, and the entity that this Import corresponds to.
        UUID importId = ( UUID ) jobExecution.getJobData().getProperty( IMPORT_ID );

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );
        Import importUG = em.get( importId, Import.class );

        //update the entity state to show that the job has officially started.
        importUG.setState(Import.State.STARTED);
        em.update( importUG );
        try {
            if ( s3PlaceHolder != null ) {
                s3Import = ( S3Import ) s3PlaceHolder;
            }
            else {
                s3Import =  new S3ImportImpl();
            }
        }
        catch ( Exception e ) {
            logger.error( "S3Import doesn't exist" );
            importUG.setErrorMessage(e.getMessage());
            importUG.setState(Import.State.FAILED);
            em.update( importUG );
            return;
        }

        if ( config.get( "organizationId" ) == null ) {
            logger.error( "No organization could be found" );
            importUG.setState(Import.State.FAILED);
            em.update( importUG );
            return;
        }
        else if ( config.get( "applicationId" ) == null ) {
            //import All the applications from an organization
            try {
                importApplicationsFromOrg((UUID) config.get("organizationId"), config, jobExecution, s3Import);
            }
            catch ( Exception e ) {
                importUG.setErrorMessage(e.getMessage());
                importUG.setState( Import.State.FAILED );
                em.update( importUG );
                return;
            }
        }
        else if ( config.get( "collectionName" ) == null ) {
            //imports an Application from a single organization
            try {
                importApplicationFromOrg((UUID) config.get("organizationId"),(UUID) config.get("applicationId"), config, jobExecution, s3Import);
            }
            catch ( Exception e ) {
                importUG.setErrorMessage( e.getMessage() );
                importUG.setState( Import.State.FAILED );
                em.update( importUG );
                return;
            }
        }
        else {

            //imports a single collection from an app org combo
            try {
                importCollectionFromOrgApp((UUID) config.get("applicationId"), config, jobExecution,s3Import);
            }
            catch ( Exception e ) {
                importUG.setErrorMessage( e.getMessage() );
                importUG.setState( Import.State.FAILED );
                em.update( importUG );
                return;
            }
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

        String appFileName = prepareInputFileName("application", application.getName(),(String) config.get("collectionName"));

        files = fileTransfer( importUG, appFileName, config, s3Import, 0 );

        FileParser();
    }

    /**
     * Imports a specific applications from an organization
     */
    private void importApplicationFromOrg( UUID organizationUUID, UUID applicationId, final Map<String, Object> config,
                                           final JobExecution jobExecution, S3Import s3Import ) throws Exception {

        //retrieves import entity
        Import importUG = getImportEntity(jobExecution);

        ApplicationInfo application = managementService.getApplicationInfo( applicationId );
        String appFileName = prepareInputFileName("application", application.getName(), null);

        files = fileTransfer( importUG, appFileName, config, s3Import, 1 );

        FileParser();
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

        appFileName = prepareInputFileName( "organization", organizationInfo.getName() , null );
        files = fileTransfer( importUG, appFileName, config, s3Import, 2 );

        FileParser();
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
        ArrayList<File> files;
        try {
            files  =  s3Import.copyFromS3(config, appFileName , type);
        }
        catch ( Exception e ) {
            importUG.setErrorMessage(e.getMessage());
            importUG.setState(Import.State.FAILED);
            return null;
        }
        return files;
    }

    /**
     * The loops through each temp file and parses it to store the entities from the json back into usergrid
     * @throws Exception
     */
    private void FileParser() throws Exception {

        for(File collectionFile : files) {
            String applicationName = collectionFile.getPath().split("\\.")[0];

            ApplicationInfo application = managementService.getApplicationInfo(applicationName);

            JsonParser jp = getJsonParserForFile(collectionFile);

            while(jp.getCurrentToken() != JsonToken.START_OBJECT) {
                jp.nextToken();
            }

            EntityManager em = emf.getEntityManager(application.getId());

            //TODO: remove roles and take care of it later when importing applications
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                importEntityStuff(jp, em);
            }
            jp.close();
        }
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
    private void importEntityStuff( JsonParser jp, EntityManager em ) throws Exception {

        EntityRef ownerEntityRef=null;
        // Go inside the value after getting the owner entity id.
        while ( jp.nextToken() != JsonToken.END_OBJECT ) {
            String collectionName = jp.getCurrentName();

            // create the connections
            if ( collectionName.equals( "connections" ) ) {

                jp.nextToken(); // START_OBJECT
                while ( jp.nextToken() != JsonToken.END_OBJECT ) {
                    String connectionType = jp.getCurrentName();

                    jp.nextToken(); // START_ARRAY
                    while ( jp.nextToken() != JsonToken.END_ARRAY ) {
                        String entryId = jp.getText();
                        
                        EntityRef entryRef = em.getRef( UUID.fromString( entryId ) );
                        // Store in DB
                        em.createConnection(ownerEntityRef, connectionType, entryRef);
                    }
                }
            }
            // add dictionaries
            else if ( collectionName.equals( "dictionaries" ) ) {

                jp.nextToken(); // START_OBJECT
                while ( jp.nextToken() != JsonToken.END_OBJECT ) {

                    String dictionaryName = jp.getCurrentName();

                    jp.nextToken();

                    @SuppressWarnings("unchecked") Map<String, Object> dictionary = jp.readValueAs( HashMap.class );

                    em.addMapToDictionary( ownerEntityRef, dictionaryName, dictionary );
                }
            }
            else {
                // Regular collections
                jp.nextToken(); // START_OBJECT

                Map<String,Object> properties = new HashMap<String,Object>();

                JsonToken token = jp.nextToken();

                while ( token != JsonToken.END_OBJECT ) {
                    if(token == JsonToken.VALUE_STRING || token == JsonToken.VALUE_NUMBER_INT )
                    {
                        String key = jp.getCurrentName();
                        if(key.equals("uuid")) {
                            ownerEntityRef = em.getRef( UUID.fromString(jp.getText()));
                        }
                        else
                        {
                            String value = jp.getText();
                            properties.put(key,value);
                        }
                    }
                    token = jp.nextToken();
                }
                // updates the properties, this indeed changes the modified property of the entity
                em.updateProperties(ownerEntityRef,properties);
            }
        }
    }

}
