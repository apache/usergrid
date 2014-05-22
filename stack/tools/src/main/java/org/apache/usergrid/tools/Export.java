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


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.tools.bean.ExportOrg;
import org.apache.usergrid.utils.JsonUtils;

import org.apache.commons.cli.CommandLine;

import com.google.common.collect.BiMap;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.index.query.Query.Level;


public class Export extends ExportingToolBase {

    static final Logger logger = LoggerFactory.getLogger( Export.class );

    JsonFactory jsonFactory = new JsonFactory();


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        setVerbose( line );

        // ExportDataCreator dataCreator = new ExportDataCreator(emf,
        // managementService);
        // dataCreator.createTestData();

        applyOrgId( line );
        prepareBaseOutputFileName( line );
        outputDir = createOutputParentDir();
        logger.info( "Export directory: " + outputDir.getAbsolutePath() );

        // Export organizations separately.
        exportOrganizations();

        // Loop through the organizations
        Map<UUID, String> organizations = getOrgs();
        for ( Entry<UUID, String> organization : organizations.entrySet() ) {

            if ( organization.equals( properties.getProperty( "usergrid.test-account.organization" ) ) ) {
                // Skip test data from being exported.
                continue;
            }

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
                logger.error( "Organization info is null!" );
                System.exit( 1 );
            }

            organizationNames = new HashMap<UUID, String>();
            organizationNames.put( orgId, info.getName() );
        }


        return organizationNames;
    }


    private void exportApplicationsForOrg( Entry<UUID, String> organization ) throws Exception {
        logger.info( "" + organization );

        // Loop through the applications per organization
        BiMap<UUID, String> applications = managementService.getApplicationsForOrganization( organization.getKey() );
        for ( Entry<UUID, String> application : applications.entrySet() ) {

            logger.info( application.getValue() + " : " + application.getKey() );

            // Get the JSon serializer.
            JsonGenerator jg = getJsonGenerator( createOutputFile( "application", application.getValue() ) );

            // load the dictionary
            EntityManager rootEm = emf.getEntityManager( emf.getManagementAppId() );

            Entity appEntity = rootEm.get( new SimpleEntityRef( "application", application.getKey()));

            Map<String, Object> dictionaries = new HashMap<String, Object>();

            for ( String dictionary : rootEm.getDictionaries( appEntity ) ) {
                Map<Object, Object> dict = rootEm.getDictionaryAsMap( appEntity, dictionary );

                // nothing to do
                if ( dict.isEmpty() ) {
                    continue;
                }

                dictionaries.put( dictionary, dict );
            }

            EntityManager em = emf.getEntityManager( application.getKey() );

            // Get application
            Entity nsEntity = em.get( new SimpleEntityRef( "application", application.getKey()));

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
            echo( JsonUtils.mapToFormattedJsonString( metadata ) );

            // Loop through the collections. This is the only way to loop
            // through the entities in the application (former namespace).
            for ( String collectionName : metadata.keySet() ) {

                Query query = new Query();
                query.setLimit( MAX_ENTITY_FETCH );
                query.setResultsLevel( Level.ALL_PROPERTIES );

                Results entities = em.searchCollection( em.getApplicationRef(), collectionName, query );

                while ( entities.size() > 0 ) {

                    for ( Entity entity : entities ) {
                        // Export the entity first and later the collections for
                        // this entity.
                        jg.writeObject( entity );
                        echo( entity );

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

            Results collectionMembers = em.getCollection( entity, collectionName, null, 100000, Level.IDS, false );

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
        jg.writeEndObject();
    }


    /** Persists the connection for this entity. */
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

            for ( Entry<Object, Object> entry : dict.entrySet() ) {
                jg.writeFieldName( entry.getKey().toString() );
                jg.writeObject( entry.getValue() );
            }

            jg.writeEndObject();
        }
        jg.writeEndObject();
    }


    /** Persists the connection for this entity. */
    private void saveConnections( Entity entity, EntityManager em, JsonGenerator jg ) throws Exception {

        jg.writeFieldName( "connections" );
        jg.writeStartObject();

        Set<String> connectionTypes = em.getConnectionTypes( entity );
        for ( String connectionType : connectionTypes ) {

            jg.writeFieldName( connectionType );
            jg.writeStartArray();

            Results results = em.getConnectedEntities( 
                    entity, connectionType, null, Level.IDS );

            List<ConnectionRef> connections = results.getConnections();

            for ( ConnectionRef connectionRef : connections ) {
                jg.writeObject( connectionRef.getConnectedEntity().getUuid() );
            }

            jg.writeEndArray();
        }
        jg.writeEndObject();
    }

  /*-
   * Set<String> collections = em.getCollections(entity);
   * for (String collection : collections) {
   *   Results collectionMembers = em.getCollection(
   *    entity, collection, null,
   *    MAX_ENTITY_FETCH, Level.IDS, false);
   *    write entity_id : { "collectionName" : [ids]
   *  }
   * }
   * 
   * 
   *   {
   *     entity_id :
   *       { collection_name :
   *         [
   *           collected_entity_id,
   *           collected_entity_id
   *         ]
   *       },
   *     f47ac10b-58cc-4372-a567-0e02b2c3d479 :
   *       { "activtites" :
   *         [
   *           f47ac10b-58cc-4372-a567-0e02b2c3d47A,
   *           f47ac10b-58cc-4372-a567-0e02b2c3d47B
   *         ]
   *       }
   *   }
   * 
   * http://jackson.codehaus.org/1.8.0/javadoc/org/codehaus/jackson/JsonGenerator.html
   * 
   *
   *-
   * List<ConnectedEntityRef> connections = em.getConnections(entityId, query);
   */


    private void exportOrganizations() throws Exception, UnsupportedEncodingException {


        for ( Entry<UUID, String> organizationName : getOrgs().entrySet() ) {

            // Let's skip the test entities.
            if ( organizationName.equals( properties.getProperty( "usergrid.test-account.organization" ) ) ) {
                continue;
            }

            OrganizationInfo acc = managementService.getOrganizationByUuid( organizationName.getKey() );
            logger.info( "Exporting Organization: " + acc.getName() );

            ExportOrg exportOrg = new ExportOrg( acc );

            List<UserInfo> users = managementService.getAdminUsersForOrganization( organizationName.getKey() );

            for ( UserInfo user : users ) {
                exportOrg.addAdmin( user.getUsername() );
            }

            // One file per Organization.
            saveOrganizationInFile( exportOrg );
        }
    }


    /**
     * Serialize an Organization into a json file.
     *
     * @param acc OrganizationInfo
     */
    private void saveOrganizationInFile( ExportOrg acc ) {
        try {

            File outFile = createOutputFile( "organization", acc.getName() );
            JsonGenerator jg = getJsonGenerator( outFile );
            jg.writeObject( acc );
            jg.close();
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }


    public void streamOutput( File file, List<Entity> entities ) throws Exception {
        JsonFactory jsonFactory = new JsonFactory();
        // or, for data binding,
        // org.codehaus.jackson.mapper.MappingJsonFactory
        JsonGenerator jg = jsonFactory.createJsonGenerator( file, JsonEncoding.UTF8 );
        // or Stream, Reader

        jg.writeStartArray();
        for ( Entity entity : entities ) {
            jg.writeObject( entity );
        }
        jg.writeEndArray();

        jg.close();
    }

    // to generate the activities and user relationship, follow this:

    // write field name (id)
    // write start object
    // write field name (collection name)
    // write start array
    // write object/string
    // write another object
    // write end array
    // write end object
    // ...... more objects
    //
}
