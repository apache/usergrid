package org.usergrid.management.export;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.impl.DefaultPrettyPrinter;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.batch.service.SchedulerService;
import org.usergrid.management.ExportInfo;
import org.usergrid.management.ManagementService;
import org.usergrid.management.OrganizationInfo;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.cassandra.CassandraService;

import com.google.common.collect.BiMap;


/**
 *
 *
 */
public class ExportServiceDos implements ExportService{

    //dependency injection
    //inject scheduler - needs to be auto wired
    @Autowired
    private SchedulerService sch;
    //inject the entity manager - needs to be auto wired
    @Autowired
    private EntityManager em;

    //injected the Entity Manager Factory
    protected EntityManagerFactory emf;

    //inject properties for organization properties
    @Autowired
    private Properties properties;

    //inject Management Service to access Organization Data
    @Autowired
    private ManagementService managementService;

    //ORG uuid .
    protected UUID orgId;

    //Maximum amount of entities retrieved in a single go.
    public static final int MAX_ENTITY_FETCH = 100;

    private JsonFactory jsonFactory = new JsonFactory();

    private String outputDir = "~/";

    protected long startTime = System.currentTimeMillis();

    protected static final String PATH_REPLACEMENT = "USERGIRD-PATH-BACKSLASH";

    //TODO: Todd, do I refactor most of the methods out to just leave schedule and doExport much like
    //the exporting toolbase class?


    @Override
    public void schedule( final ExportInfo config ) {
        //SchedulerServiceImpl sch =

    }


    @Override
    public void doExport( final ExportInfo config ) throws Exception {

        Map<UUID, String> organizations = getOrgs();
        for ( Map.Entry<UUID, String> organization : organizations.entrySet() ) {

//            if ( organization.equals( properties.getProperty( "usergrid.test-account.organization" ) ) ) {
//                // Skip test data from being exported.
//                continue;
//            }

            exportApplicationsForOrg( organization );
        }
    }

    private Map<UUID, String> getOrgs() throws Exception {
        // Loop through the organizations
        Map<UUID, String> organizationNames = null;

        if ( orgId == null ) {
            organizationNames = managementService.getOrganizations();
        }

        else {
            OrganizationInfo info = managementService.getOrganizationByUuid( orgId );

            if ( info == null ) {

                //logger.error( "Organization info is null!" );
                System.exit( 1 );
            }

            organizationNames = new HashMap<UUID, String>();
            organizationNames.put( orgId, info.getName() );
        }


        return organizationNames;
    }

    private void exportApplicationsForOrg( Map.Entry<UUID, String> organization ) throws Exception {

        Logger logger = LoggerFactory.getLogger( ExportServiceDos.class );

        logger.info( "" + organization );


        // Loop through the applications per organization
        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( organization.getKey() );
        for ( Map.Entry<UUID, String> application : applications.entrySet() ) {

            logger.info( application.getValue() + " : " + application.getKey() );

            // Get the JSon serializer.
           // JsonGenerator jg =
            JsonGenerator jg = getJsonGenerator( createOutputFile( "application", application.getValue() ) );



            // load the dictionary

            EntityManager rootEm = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );

            Entity appEntity = rootEm.get( application.getKey() );

            Map<String, Object> dictionaries = new HashMap<String, Object>();

            for ( String dictionary : rootEm.getDictionaries( appEntity ) ) {
                Map<Object, Object> dict = rootEm.getDictionaryAsMap( appEntity, dictionary );

                // nothing to do
                if ( dict.isEmpty() ) {
                    continue;
                }

                dictionaries.put( dictionary, dict );
            }
            //TODO: resolve problem that look similar to this.
            EntityManager em = emf.getEntityManager( application.getKey() );

            // Get application
            Entity nsEntity = em.get( application.getKey() );

            Set<String> collections = em.getApplicationCollections();

            // load app counters

            Map<String, Long> entityCounters = em.getApplicationCounters();

            nsEntity.setMetadata( "organization", organization );
            nsEntity.setMetadata( "dictionaries", dictionaries );
            // counters for collections
            nsEntity.setMetadata( "counters", entityCounters );
            nsEntity.setMetadata( "collections", collections );

            jg.writeStartArray();
            jg.writeObject( nsEntity );

            // Create a GENERATOR for the application collections.
            JsonGenerator collectionsJg = getJsonGenerator( createOutputFile( "collections", application.getValue() ) );
            collectionsJg.writeStartObject();

            Map<String, Object> metadata = em.getApplicationCollectionMetadata();
            //don't need to echo as not a command line tool anymore
            //echo( JsonUtils.mapToFormattedJsonString( metadata ) );

            // Loop through the collections. This is the only way to loop
            // through the entities in the application (former namespace).
            for ( String collectionName : metadata.keySet() ) {

                Query query = new Query();
                query.setLimit( MAX_ENTITY_FETCH );
                query.setResultsLevel( Results.Level.ALL_PROPERTIES );

                Results entities = em.searchCollection( em.getApplicationRef(), collectionName, query );

                while ( entities.size() > 0 ) {

                    for ( Entity entity : entities ) {
                        // Export the entity first and later the collections for
                        // this entity.
                        jg.writeObject( entity );
                        //echo( entity );

                        saveCollectionMembers( collectionsJg, em, application.getValue(), entity );
                    }

                    //we're done
                    if ( entities.getCursor() == null ) {
                        break;
                    }


                    query.setCursor( entities.getCursor() );

                    entities = em.searchCollection( em.getApplicationRef(), collectionName, query );
                }
            }

            // Close writer for the collections for this application.
            collectionsJg.writeEndObject();
            collectionsJg.close();

            // Close writer and file for this application.
            jg.writeEndArray();
            jg.close();
        }
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

        // Only create entry for Entities that have collections
        if ( ( collections == null ) || collections.isEmpty() ) {
            return;
        }

        jg.writeFieldName( entity.getUuid().toString() );
        jg.writeStartObject();

        for ( String collectionName : collections ) {

            jg.writeFieldName( collectionName );
            // Start collection array.
            jg.writeStartArray();

            Results collectionMembers = em.getCollection( entity, collectionName, null, 100000, Results.Level.IDS, false );

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
        //saveConnections( entity, em, jg );

        // Write dictionaries
        //saveDictionaries( entity, em, jg );

        // End the object if it was Started
        jg.writeEndObject();
    }

    protected JsonGenerator getJsonGenerator( String outFile ) throws IOException {
        return getJsonGenerator( new File( outputDir, outFile ) );
    }


    protected JsonGenerator getJsonGenerator( File outFile ) throws IOException {
        PrintWriter out = new PrintWriter( outFile, "UTF-8" );
        JsonGenerator jg = jsonFactory.createJsonGenerator( out );
        jg.setPrettyPrinter( new DefaultPrettyPrinter() );
        jg.setCodec( new ObjectMapper() );
        return jg;
    }

    protected File createOutputFile( String type, String name ) {
        return new File( outputDir, prepareOutputFileName( type, name ) );
    }


    /**
     * @param type just a label such us: organization, application.
     *
     * @return the file name concatenated with the type and the name of the collection
     */
    protected String prepareOutputFileName( String type, String name ) {
        name = name.replace( "/", PATH_REPLACEMENT );
        // Add application and timestamp
        StringBuilder str = new StringBuilder();
        // str.append(baseOutputFileName);
        // str.append(".");
        str.append( type );
        str.append( "." );
        str.append( name );
        str.append( "." );
        str.append( startTime );
        str.append( ".json" );

        String outputFileName = str.toString();

        //logger.info( "Creating output filename:" + outputFileName );

        return outputFileName;
    }

}
