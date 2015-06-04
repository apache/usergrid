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
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;


/**
 * TODO: REFACTOR EVERYTHING TO USE JSON NODES
 * Example on how to run:
 * java -jar usergrid-tools.jar ImportAdmins -host cassandraHost -v -inputDir exportFilesDirectory
 */
public class ImportAdmins extends ToolBase {

    private static final Logger logger = LoggerFactory.getLogger( ImportAdmins.class );

    /**
     * Input directory where the .json export files are
     */
    static final String INPUT_DIR = "inputDir";

    static File importDir;

    static final String DEFAULT_INPUT_DIR = "export";

    JsonFactory jsonFactory = new JsonFactory();


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {

        Option hostOption = OptionBuilder.withArgName( "host" )
                .hasArg().withDescription( "Cassandra host" ).create( "host" );

        Option inputDir = OptionBuilder
                .hasArg().withDescription( "input directory -inputDir" ).create( INPUT_DIR );

        Option verbose = OptionBuilder
                .withDescription( "Print on the console an echo of the content written to the file" )
                .create( VERBOSE );

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( inputDir );
        options.addOption( verbose );

        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {

        startSpring();

        setVerbose( line );

        openImportDirectory( line );

        importAdminUsers();

        importMetadata();

        // forces the counters to flush
//        logger.info( "Sleeping 35 seconds for batcher" );
//        Thread.sleep( 35000 );
    }


    /**
     * Import admin users.
     */
    private void importAdminUsers() throws Exception {
        String[] fileNames = importDir.list( new PrefixFileFilter( ExportAdmins.ADMIN_USERS_PREFIX + "." ) );
        logger.info( "Applications to read: " + fileNames.length );

        //this fails on the second run of the applications find out why.
        for ( String fileName : fileNames ) {
            try {
                importAdminUsers( fileName );
            }
            catch ( Exception e ) {
                logger.warn( "Unable to import application: " + fileName, e );
            }
        }
    }


    /**
     * Imports admin users.
     *
     * @param fileName Name of admin user data file.
     */
    private void importAdminUsers( String fileName ) throws Exception {

        int count = 0;

        File adminUsersFile = new File( importDir, fileName );

        logger.info( "----- Loading file: " + adminUsersFile.getAbsolutePath() );
        JsonParser jp = getJsonParserForFile( adminUsersFile );

        JsonToken token = jp.nextToken();
        validateStartArray( token );

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        while ( jp.nextValue() != JsonToken.END_ARRAY ) {

            @SuppressWarnings( "unchecked" )
            Map<String, Object> entityProps = jp.readValueAs( HashMap.class );

            // Import/create the entity
            UUID uuid = getId( entityProps );
            String type = getType( entityProps );


            try {
                em.create( uuid, type, entityProps );

                logger.debug( "Imported admin user {} {}", uuid, entityProps.get( "username" ) );
                count++;
                if ( count % 1000 == 0 ) {
                    logger.info("Imported {} admin users", count);
                }
            }
            catch ( DuplicateUniquePropertyExistsException de ) {
                logger.warn( "Unable to create entity. It appears to be a duplicate: " +
                    "id={}, type={}, name={}, username={}",
                    new Object[] { uuid, type, entityProps.get("name"), entityProps.get("username")});
                if ( logger.isDebugEnabled() ) {
                    logger.debug( "Exception" , de );
                }
                continue;
            }

            if ( em.get( uuid ) == null ) {
                logger.error( "Holy hell, we wrote an entity and it's missing.  " +
                                "Entity Id was {} and type is {}", uuid, type );
                System.exit( 1 );
            }
            echo( entityProps );
        }

        logger.info( "----- End: Imported {} admin users from file {}",
                count, adminUsersFile.getAbsolutePath() );

        jp.close();
    }


    private String getType( Map<String, Object> entityProps ) {
        return ( String ) entityProps.get( PROPERTY_TYPE );
    }


    private UUID getId( Map<String, Object> entityProps ) {
        return UUID.fromString( ( String ) entityProps.get( PROPERTY_UUID ) );
    }


    private void validateStartArray( JsonToken token ) {
        if ( token != JsonToken.START_ARRAY ) {
            throw new RuntimeException( "Token should be START ARRAY but it is:" + token.asString() );
        }
    }


    private JsonParser getJsonParserForFile( File organizationFile ) throws Exception {
        JsonParser jp = jsonFactory.createJsonParser( organizationFile );
        jp.setCodec( new ObjectMapper() );
        return jp;
    }


    /**
     * Import collections. Collections files are named: collections.<application_name>.Timestamp.json
     */
    private void importMetadata() throws Exception {

        String[] fileNames = importDir.list(
                new PrefixFileFilter( ExportAdmins.ADMIN_USER_METADATA_PREFIX + "." ) );
        logger.info( "Metadata files to read: " + fileNames.length );

        for ( String fileName : fileNames ) {
            try {
                importMetadata( fileName );
            }
            catch ( Exception e ) {
                logger.warn( "Unable to import metadata file: " + fileName, e );
            }
        }
    }


    @SuppressWarnings( "unchecked" )
    private void importMetadata( String fileName ) throws Exception {

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        File metadataFile = new File( importDir, fileName );

        logger.info( "----- Loading metadata file: " + metadataFile.getAbsolutePath() );

        JsonParser jp = getJsonParserForFile( metadataFile );

        JsonToken jsonToken = null; // jp.nextToken();// START_OBJECT this is the outer hashmap

        int depth = 1;

        while ( depth > 0 ) {

            jsonToken = jp.nextToken();

            if ( jsonToken == null ) {
                logger.info("token is null, breaking");
                break;
            }

            if (jsonToken.equals( JsonToken.START_OBJECT )) {
                depth++;
            } else if (jsonToken.equals( JsonToken.END_OBJECT )) {
                depth--;
            }

            if (jsonToken.equals( JsonToken.FIELD_NAME ) && depth == 2 ) {

                jp.nextToken();

                String entityOwnerId = jp.getCurrentName();
                EntityRef entityRef = em.getRef( UUID.fromString( entityOwnerId ) );

                Map<String, Object> metadata = (Map<String, Object>)jp.readValueAs( Map.class );
                importEntityMetadata( em, entityRef, metadata );
            }
        }

        logger.info( "----- End of metadata -----" );
        jp.close();
    }


    /**
     * Imports the entity's connecting references (collections and connections)
     */
    @SuppressWarnings("unchecked")
    private void importEntityMetadata(
        EntityManager em, EntityRef entityRef, Map<String, Object> metadata ) throws Exception {

        Map<String, Object> connectionsMap = (Map<String, Object>) metadata.get( "connections" );

        if (connectionsMap != null && !connectionsMap.isEmpty()) {
            for (String type : connectionsMap.keySet()) {
                try {
                    UUID uuid = UUID.fromString( (String) connectionsMap.get( type ) );
                    EntityRef connectedEntityRef = em.getRef( uuid );
                    em.createConnection( entityRef, type, connectedEntityRef );

                    logger.debug( "Creating connection from {} type {} target {}",
                            new Object[]{entityRef, type, connectedEntityRef});

                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error( "Error importing connection of type "
                                + type + " for user " + entityRef.getUuid(), e );
                    } else {
                        logger.error( "Error importing connection of type "
                                + type + " for user " + entityRef.getUuid() );
                    }
                }
            }
        }

        Map<String, Object> dictionariesMap = (Map<String, Object>) metadata.get( "dictionaries" );

        if (dictionariesMap != null && !dictionariesMap.isEmpty()) {
            for (String name : dictionariesMap.keySet()) {
                try {
                    Map<String, Object> dictionary = (Map<String, Object>) dictionariesMap.get( name );
                    em.addMapToDictionary( entityRef, name, dictionary );

                    logger.debug("Creating dictionary for {} name {} map {}",
                            new Object[] { entityRef, name, dictionary  });

                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error( "Error importing dictionary name "
                                + name + " for user " + entityRef.getUuid(), e );
                    } else {
                        logger.error( "Error importing dictionary name "
                                + name + " for user " + entityRef.getUuid() );
                    }
                }
            }
        }

        List<String> collectionsList = (List<String>) metadata.get( "collections" );
        if (collectionsList != null && !collectionsList.isEmpty()) {
            for (String name : collectionsList) {
                try {
                    UUID uuid = UUID.fromString( (String) connectionsMap.get( name ) );
                    EntityRef collectedEntityRef = em.getRef( uuid );
                    em.addToCollection( entityRef, name, collectedEntityRef );

                    logger.debug("Add to collection of {} name {} entity {}",
                            new Object[] { entityRef, name, collectedEntityRef });

                } catch (Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.error( "Error adding to collection "
                                + name + " for user " + entityRef.getUuid(), e );
                    } else {
                        logger.error( "Error adding to collection "
                                + name + " for user " + entityRef.getUuid() );
                    }
                }
            }
        }


        List<Object> organizationsList = (List<Object>) metadata.get( "organizations" );
        if (organizationsList != null && !organizationsList.isEmpty()) {

            for ( Object orgObject : organizationsList ) {

                Map<String, Object> orgMap = (Map<String, Object>)orgObject;
                UUID orgUuid = UUID.fromString( (String)orgMap.get("uuid") );
                String orgName = (String)orgMap.get("name");

                User user = em.get( entityRef, User.class );
                final UserInfo userInfo = managementService.getAdminUserByEmail( user.getEmail() );

                // create org only if it does not exist
                OrganizationInfo orgInfo = managementService.getOrganizationByUuid( orgUuid );
                if ( orgInfo == null ) {
                    try {
                        managementService.createOrganization( orgUuid, orgName, userInfo, false );
                        orgInfo = managementService.getOrganizationByUuid( orgUuid );

                        logger.debug( "Created new org {} for user {}",
                            new Object[]{orgInfo.getName(), user.getEmail()} );

                    } catch (DuplicateUniquePropertyExistsException dpee ) {
                        logger.error("Org {} already exists", orgName );
                    }
                } else {
                    managementService.addAdminUserToOrganization( userInfo, orgInfo, false );
                    logger.debug( "Added user {} to org {}", new Object[]{user.getEmail(), orgName} );
                }
            }
        }
    }


    /**
     * Open up the import directory based on <code>importDir</code>
     */
    private void openImportDirectory( CommandLine line ) {

        boolean hasInputDir = line.hasOption( INPUT_DIR );

        if ( hasInputDir ) {
            importDir = new File( line.getOptionValue( INPUT_DIR ) );
        }
        else {
            importDir = new File( DEFAULT_INPUT_DIR );
        }

        logger.info( "Importing from:" + importDir.getAbsolutePath() );
        logger.info( "Status. Exists: " + importDir.exists() + " - Readable: " + importDir.canRead() );
    }
}
