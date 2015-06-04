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


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.BiMap;
import org.codehaus.jackson.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;

import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Results.Level;
import org.apache.usergrid.persistence.cassandra.CassandraService;


/**
 * java -jar usergrid-tools.jar ExportAdmins
 */
public class ExportAdmins extends ExportingToolBase {

    static final Logger logger = LoggerFactory.getLogger( ExportAdmins.class );

    public static final String ADMIN_USERS_PREFIX = "admin-users";
    public static final String ADMIN_USER_METADATA_PREFIX = "admin-user-metadata";

    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        setVerbose( line );

        applyOrgId( line );
        prepareBaseOutputFileName( line );
        outputDir = createOutputParentDir();
        logger.info( "Export directory: " + outputDir.getAbsolutePath() );

        exportAdminUsers();
    }


    private void exportAdminUsers() throws Exception {

        int count = 0;

        EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );

        // write one JSON file for management application users

        JsonGenerator usersFile =
                getJsonGenerator( createOutputFile( ADMIN_USERS_PREFIX, em.getApplication().getName() ) );
        usersFile.writeStartArray();

        // write one JSON file for metadata: collections, connections and dictionaries of those users

        JsonGenerator metadataFile =
                getJsonGenerator( createOutputFile( ADMIN_USER_METADATA_PREFIX, em.getApplication().getName() ) );
        metadataFile.writeStartObject();

        // query for and loop through all users in management application

        Query query = new Query();
        query.setLimit( MAX_ENTITY_FETCH );
        query.setResultsLevel( Results.Level.ALL_PROPERTIES );

        Results entities = em.searchCollection( em.getApplicationRef(), "users", query );

        while ( entities.size() > 0 ) {

            for ( Entity entity : entities ) {

                // write user to application file
                usersFile.writeObject( entity );
                echo( entity );

                // write user's collections, connections, etc. to collections file
                saveEntityMetadata( metadataFile, em, null, entity );

                logger.debug("Exported user {}", entity.getProperty( "email" ));

                count++;
                if ( count % 1000 == 0 ) {
                    logger.info("Exported {} admin users", count);
                }

            }

            if ( entities.getCursor() == null ) {
                break;
            }
            query.setCursor( entities.getCursor() );
            entities = em.searchCollection( em.getApplicationRef(), "users", query );
        }

        metadataFile.writeEndObject();
        metadataFile.close();

        usersFile.writeEndArray();
        usersFile.close();

        logger.info("Exported total of {} admin users", count);
    }


    /**
     * Serialize and save the collection members of this <code>entity</code>
     *
     * @param em Entity Manager
     * @param application Application name
     * @param entity entity
     */
    private void saveEntityMetadata(
            JsonGenerator jg, EntityManager em, String application, Entity entity) throws Exception {

        saveCollections( jg, em, entity );
        saveConnections( entity, em, jg );
        saveOrganizations( entity, em, jg );
        saveDictionaries( entity, em, jg );

        // End the object if it was Started
        jg.writeEndObject();
    }


    private void saveCollections(JsonGenerator jg, EntityManager em, Entity entity) throws Exception {

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
     * Persists the outgoing connections for this entity.
     */
    private void saveConnections( Entity entity, EntityManager em, JsonGenerator jg ) throws Exception {

        jg.writeFieldName( "connections" );
        jg.writeStartObject();

        Set<String> connectionTypes = em.getConnectionTypes( entity );
        for ( String connectionType : connectionTypes ) {

            jg.writeFieldName( connectionType );
            jg.writeStartArray();

            Results results = em.getConnectedEntities( entity.getUuid(), connectionType, null, Level.IDS );
            List<ConnectionRef> connections = results.getConnections();

            for ( ConnectionRef connectionRef : connections ) {
                jg.writeObject( connectionRef.getConnectedEntity().getUuid() );
            }

            jg.writeEndArray();
        }
        jg.writeEndObject();
    }


    /**
     * Persists the incoming connections for this entity.
     */
    private void saveOrganizations(Entity entity, EntityManager em, JsonGenerator jg) throws Exception {

        final BiMap<UUID, String> orgs = managementService.getOrganizationsForAdminUser( entity.getUuid() );

        jg.writeFieldName( "organizations" );

        jg.writeStartArray();

        for ( UUID uuid : orgs.keySet() ) {

             jg.writeStartObject();

             jg.writeFieldName( "uuid" );
             jg.writeObject( uuid );

             jg.writeFieldName( "name" );
             jg.writeObject( orgs.get( uuid ) );

             jg.writeEndObject();
        }

        jg.writeEndArray();
    }

}

