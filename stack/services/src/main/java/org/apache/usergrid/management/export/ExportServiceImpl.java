package org.apache.usergrid.management.export;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
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
import org.apache.usergrid.management.ExportInfo;

import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.PagingResultsIterator;
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
    public static final int MAX_ENTITY_FETCH = 100;

    //Amount of time that has passed before sending another heart beat in millis
    public static final int TIMESTAMP_DELTA = 5000;

    private JsonFactory jsonFactory = new JsonFactory();

    protected long startTime = System.currentTimeMillis();

    private S3Export s3Export;


    @Override
    public UUID schedule( final ExportInfo config ) throws Exception {

        EntityManager em = emf.getEntityManager( config.getApplicationId() );

        Export export = new Export();
        export.setState( Export.State.PENDING );

        //validate that org exists,then app, then collection.

        String pathToBeParsed = config.getPath();
        //split the path so that you can verify that the organization and the app exist.
        String[] pathItems = pathToBeParsed.split( "/" );


        try {
            managementService.getOrganizationByName( pathItems[0] );
        }
        catch ( Exception e ) {
            logger.error( "Organization doesn't exist" );
        }

        try {
            managementService.getApplicationInfo( pathItems[1] );
        }
        catch ( Exception e ) {
            logger.error( "Application doesn't exist" );
        }


        //TODO: parse path and make sure all the things you need actually exist. then throw
        // good error messages when not found.

        //write to the em
        export = em.create( export );
        export.setState( Export.State.PENDING );
        em.update( export );

        JobData jobData = new JobData();
        jobData.setProperty( "exportInfo", config );
        jobData.setProperty( EXPORT_ID, export.getUuid() );

        long soonestPossible = System.currentTimeMillis() + 250; //sch grace period

        sch.createJob( EXPORT_JOB_NAME, soonestPossible, jobData );

        return export.getUuid();
    }


    /**
     * Query Entity Manager for specific Export Entity within application
     *
     * @return String
     */
    @Override
    public String getState( final UUID appId, final UUID uuid ) throws Exception {

        EntityManager rootEm = emf.getEntityManager( appId );
        Export export = rootEm.get( uuid, Export.class );

        if ( export == null ) {
            return null;
        }
        return export.getState().toString();
    }


    @Override
    public void doExport( final ExportInfo config, final JobExecution jobExecution ) throws Exception {

        UUID exportId = ( UUID ) jobExecution.getJobData().getProperty( EXPORT_ID );
        EntityManager em = emf.getEntityManager( config.getApplicationId() );
        Export export = em.get( exportId, Export.class );

        String pathToBeParsed = config.getPath();
        String[] pathItems = pathToBeParsed.split( "/" );
        try {
            managementService.getOrganizationByName( pathItems[0] );
        }
        catch ( Exception e ) {
            logger.error( "Organization doesn't exist" );
            return;
        }

        //update state and re-write the entity
        export.setState( Export.State.STARTED );

        em.update( export );

        Map<UUID, String> organizationGet = getOrgs( config );
        for ( Map.Entry<UUID, String> organization : organizationGet.entrySet() ) {
            //needs to pass app name, and possibly collection to export
            exportApplicationsForOrg( organization, config, jobExecution );
        }
        export.setState( Export.State.COMPLETED );
        em.update( export );
    }


    private Map<UUID, String> getOrgs( ExportInfo exportInfo ) throws Exception {
        // Loop through the organizations
        // TODO:this will come from the orgs in schedule when you do the validations. delete orgId
        UUID orgId = null;

        Map<UUID, String> organizationNames = null;

        if ( orgId == null ) {
            organizationNames = managementService.getOrganizations();
        }

        else {
            OrganizationInfo info = managementService.getOrganizationByUuid( orgId );

            if ( info == null ) {
                logger.error( "Organization info is null!" );
            }

            organizationNames = new HashMap<UUID, String>();
            organizationNames.put( orgId, info.getName() );
        }


        return organizationNames;
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


    //write test checking to see what happens if the input stream is closed or wrong.
    //TODO: make multipart streaming functional
    //currently only stores the collection in memory then flushes it.
    private void exportApplicationsForOrg( Map.Entry<UUID, String> organization, final ExportInfo config,
                                           final JobExecution jobExecution ) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        logger.info( "" + organization );

        // Loop through the applications per organization
        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( organization.getKey() );
        for ( Map.Entry<UUID, String> application : applications.entrySet() ) {

            logger.info( application.getValue() + " : " + application.getKey() );

            String appFileName = prepareOutputFileName( "application", application.getValue() );

            JsonGenerator jg = getJsonGenerator( baos );

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

                Query query = new Query();
                query.setLimit( MAX_ENTITY_FETCH );
                query.setResultsLevel( Results.Level.ALL_PROPERTIES );
                Results entities = em.searchCollection( em.getApplicationRef(), collectionName, query );

                PagingResultsIterator itr = new PagingResultsIterator( entities );

                for ( Object e : itr ) {
                    starting_time = checkTimeDelta( starting_time, jobExecution );
                    Entity entity = ( Entity ) e;

                    jg.writeStartObject();
                    jg.writeFieldName( "Metadata" );
                    jg.writeObject( entity );
                    saveCollectionMembers( jg, em, application.getValue(), entity );
                    jg.writeEndObject();
                }
            }

            // Close writer and file for this application.
            jg.writeEndArray();
            jg.close();
            baos.flush();
            baos.close();

            InputStream is = new ByteArrayInputStream( baos.toByteArray() );
            s3Export.copyToS3( is, config, appFileName );
        }
    }


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
     * @param application Application name
     * @param entity entity
     */
    private void saveCollectionMembers( JsonGenerator jg, EntityManager em, String application, Entity entity )
            throws Exception {

        Set<String> collections = em.getCollections( entity );
        //jg.writeStartObject();

        // Only create entry for Entities that have collections
        if ( ( collections == null ) || collections.isEmpty() ) {
            return;
        }


        for ( String collectionName : collections ) {

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

        // Write connections
        saveConnections( entity, em, jg );

        // Write dictionaries
        saveDictionaries( entity, em, jg );

        // End the object if it was Started
        //jg.writeEndObject();
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


    protected File createOutputFile( String type, String name ) {
        return new File( prepareOutputFileName( type, name ) );
    }


    /**
     * @param type just a label such us: organization, application.
     *
     * @return the file name concatenated with the type and the name of the collection
     */
    protected String prepareOutputFileName( String type, String name ) {
        StringBuilder str = new StringBuilder();
        str.append( name );
        str.append( "." );
        str.append( startTime );
        str.append( ".json" );

        String outputFileName = str.toString();
        //TODO:this is , i feel, bad practice so make sure to come back here and fix it up.

        return outputFileName;
    }


    @Autowired
    @Override
    public void setS3Export( S3Export s3Export ) { this.s3Export = s3Export; }
}
