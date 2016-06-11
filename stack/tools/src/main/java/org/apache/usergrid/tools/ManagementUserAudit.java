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
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;

import com.google.common.collect.BiMap;

import me.prettyprint.hector.api.beans.HColumn;

import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;


/**
 * This utility audits all values in the ENTITY_UNIQUE column family. If it finds any duplicates of users then it
 * deletes the non existing columns from the row. If there are no more columns in the row then it deletes the row. If
 * there exists more than one existing column then the one with the most recent timestamp wins and the other is
 * deleted.
 *
 * If you want the run the tool on their cluster the following is what you need to do nohup java
 * -Dlog4j.configuration=file:log4j.properties -jar usergrid-tools-1.0.2.jar UserUniqueIndexCleanup -host
 * <cassandra_host_here>  > log.txt
 *
 * if there is a specific value you want to run the tool on then you need the following
 *
 * nohup java -Dlog4j.configuration=file:log4j.properties -jar usergrid-tools-1.0.2.jar UserUniqueIndexCleanup -host
 * <cassandra_host_here> -app <applicationUUID> -col <collection_name> -property <unique_property_key> -value
 * <unique_property_value> > log.txt
 *
 * @author grey
 */
public class ManagementUserAudit extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( ManagementUserAudit.class );

    private static final String FILE_PATH = "file";

    private static final String DUPLICATE_EMAIL = "dup";

    private static final String ROW_KEY = "row";


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = new Options();

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        options.addOption( hostOption );

        Option file_path =
                OptionBuilder.withArgName( FILE_PATH ).hasArg().isRequired( false ).withDescription( "file path" )
                             .create( FILE_PATH );
        options.addOption( file_path );

        Option duplicate_email = OptionBuilder.withArgName( DUPLICATE_EMAIL ).hasArg().isRequired( false )
                                              .withDescription( "duplicate email to examine" )
                                              .create( DUPLICATE_EMAIL );
        options.addOption( duplicate_email );

        Option row_key = OptionBuilder.withArgName( ROW_KEY ).hasArg().isRequired( false )
                                      .withDescription( "row key to check against" ).create( ROW_KEY );
        options.addOption( row_key );

        return options;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();

        logger.info( "Starting entity unique checker" );

        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        if ( line.getOptionValue( ( "file" ) ) == null ) {
            if ( line.getOptionValue( "dup" ) != null ) {
                String extractedEmail = line.getOptionValue( "dup" );
                column_verification( em, extractedEmail, line );
            }
            else {
                logger.error( "Need to have -file or -dup not both and certainly not neither." );
            }
        }
        else {
            ObjectMapper objectMapper = new ObjectMapper();

            File jsonObjectFile = new File( line.getOptionValue( "file" ) );

            JsonNode node = objectMapper.readTree( jsonObjectFile );

            JsonNode users = node.get( "user" );

            for ( JsonNode email : users ) {

                String extractedEmail = email.get( "name" ).getTextValue();
                column_verification( em, extractedEmail, line );
            }
            logger.info( "Completed logging successfully" );
        }
    }


    private void column_verification( final EntityManager em, final String extractedEmail, CommandLine line )
            throws Exception {
        UUID applicationId = MANAGEMENT_APPLICATION_ID;
        String collectionName = "users";
        String uniqueValueKey = "email";
        String uniqueValue = extractedEmail;


        Object key = key( applicationId, collectionName, uniqueValueKey, uniqueValue );

        EntityManagerImpl emi = ( EntityManagerImpl ) em;


        List<HColumn<ByteBuffer, ByteBuffer>> cols =
                cass.getAllColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_UNIQUE, key, be, be );

        if ( cols.size() == 1 ) {
            UUID uuid = null;
            for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
                uuid = ue.fromByteBuffer( col.getName() );
            }
            if ( em.get( uuid ) == null ) {
                logger.error( "Email: {} with uuid: {} doesn't exist in ENTITY_PROPERTIES.", extractedEmail, uuid );
            }
            else {
                logger.info( "Email: {}  with uuid: {} exists in ENTITY_PROPERTIES for ENTITY_UNIQUE", extractedEmail,
                        uuid );
            }
        }
        else {
            if ( cols.size() == 0 ) {
                logger.error( "Email: {} doesn't exist in ENTITY_UNIQUE.", extractedEmail );
            }
            else {
                logger.warn( "Email: {} has {} number of duplicate columns in ENTITY_UNIQUE.Attempting Repair.",
                        extractedEmail, cols.size() );
                UUID uuid = null;

                //Only handles the case if there are two duplicates, if there are more then expand the tool. None
                // have been found so far.

                //if(cols.size()==2){
                uuid = ue.fromByteBuffer( cols.get( 0 ).getName() );
                Entity entity = em.get( uuid );
                Long created = entity.getCreated();
                UserInfo adminUserInfo = managementService.getAdminUserByUuid( uuid );


                for ( int index = 1; index < cols.size(); index++ ) {
                    UUID duplicateEmailUUID = ue.fromByteBuffer( cols.get( index ).getName() );
                    Entity duplicateEntity = em.get( duplicateEmailUUID );

                    //if the dup is newer than the first entry
                    if ( created < duplicateEntity.getCreated() ) {
                        BiMap<UUID, String> uuidStringOrgBiMap =
                                managementService.getOrganizationsForAdminUser( duplicateEmailUUID );
                        BiMap<String, UUID> stringUUIDBiMap = uuidStringOrgBiMap.inverse();
                        for ( String orgName : stringUUIDBiMap.keySet() ) {
                            logger.warn( "Adding admin user: {} to organization: {}", adminUserInfo, orgName );
                                                            OrganizationInfo organizationInfo = managementService
                             .getOrganizationByUuid( stringUUIDBiMap.get( orgName ) );
                                                            managementService.addAdminUserToOrganization
                             (adminUserInfo,organizationInfo , false );
                        }
                        logger.warn( "Deleting duplicated uuid: {}.", duplicateEmailUUID );
                                                    emi.deleteEntity( duplicateEmailUUID );
                    }
                    else if ( created > duplicateEntity.getCreated() ) {
                        logger.info("older column was returned later from cassandra");
                        BiMap<UUID, String> uuidStringOrgBiMap = managementService.getOrganizationsForAdminUser( uuid );
                        BiMap<String, UUID> stringUUIDBiMap = uuidStringOrgBiMap.inverse();
                        adminUserInfo = managementService.getAdminUserByUuid( duplicateEmailUUID );


                        for ( String orgName : stringUUIDBiMap.keySet() ) {
                            logger.warn( "Adding admin user: {} to organization: {}", adminUserInfo, orgName );
                            OrganizationInfo organizationInfo = managementService.getOrganizationByUuid(
                             stringUUIDBiMap.get( orgName ) );
                            managementService.addAdminUserToOrganization(adminUserInfo,organizationInfo , false );
                        }
                        logger.warn( "Deleting duplicated uuid: {}.", uuid );
                        emi.deleteEntity( uuid );
                        created = duplicateEntity.getCreated();
                        uuid = duplicateEmailUUID;
                    }
                }

                //                }

                //                for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
                //                    uuid = ue.fromByteBuffer( col.getName() );
                //                    //managementService.get
                //                    entity = em.get( uuid );
                //                    adminUserInfo = managementService.getAdminUserByUuid( uuid );
                //
                //                    Map<String, Object>
                //                            userOrganizationData = managementService.getAdminUserOrganizationData(
                // adminUserInfo, true );
                //
                //
                //                    if ( entity == null ) {
                //                        logger.error( "Email: {} with duplicate uuid: {} doesn't exist in
                // ENTITY_PROPERTIES.",
                //                                extractedEmail, uuid );
                //                    }
                //                    else {
                //
                //
                //
                //
                //                        Object[] loggerObject = new Object[4];
                //                        loggerObject[0] = extractedEmail;
                //                        loggerObject[1] = uuid;
                //                        loggerObject[2] = adminUserInfo;
                //                        loggerObject[3] = userOrganizationData;
                //                        logger.warn( "Email: {}  with duplicate uuid: {} with the following data:
                // {} and organizational data: {} exists in "
                //                                + "ENTITY_PROPERTIES", loggerObject);
                //

                //                        Set<String> dictionaries = emi.getDictionaryNames( entity );
                //                        if ( dictionaries != null ) {
                //                            for ( String dictionary : dictionaries ) {
                //                                Set<Object> values = emi.getDictionaryAsSet( entity, dictionary );
                //                                logger.warn("The uuid: {} has the following dictionary: {} with the following values: {}.",new Object[] {uuid,dictionary,values});
                //                                for ( Object value : values ) {
                //                                    emi.batchUpdateDictionary( m, entity, dictionary, value, true, timestampUuid );
                //                                }
                //                            }
                //                        }
                //                    }
            }
        }
    }
}
//}
