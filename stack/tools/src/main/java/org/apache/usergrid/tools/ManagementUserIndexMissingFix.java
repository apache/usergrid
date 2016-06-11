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
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.RelationManager;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;
import org.apache.usergrid.persistence.cassandra.IndexUpdate;
import org.apache.usergrid.persistence.cassandra.RelationManagerImpl;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.query.ir.result.ScanColumn;
import org.apache.usergrid.persistence.query.ir.result.SecondaryIndexSliceParser;
import org.apache.usergrid.persistence.schema.CollectionInfo;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.cassandra.service.RangeSlicesIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.Results.Level.REFS;
import static org.apache.usergrid.persistence.SimpleEntityRef.ref;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.createTimestamp;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.CassandraService.dce;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;


/**
 * This utility audits all values in the ENTITY_UNIQUE column family. The way it does this is by taking in a *.json file
 * that has an array of users then checking them to see if they are in the unique column family. Logging as it goes along.
 *
 * An additional use of this tool is to use the -dup flag like so
 * java -Dlog4j.configuration=file:log4j.properties  -jar usergrid-tools-1.0.2.jar ManagementUserAudit -host localhost -dup grey@apache.org
 *
 * If we find a value that is present and also exists in ENTITY_PROPERTIES then it goes and queries the entity index.
 * The reason this check has to be done is that it can be possible for an entity to have an invalid uuid in the entity index
 * but has the correct data in ENTITY_UNIQUE. Causing a user to be unable to login.
 * If they are in the index then we see if the uuid's that are stored in the index correspond to any entity in the ENTITY_PROPERTIES
 * column family. If it does not then we retreive the column name from the RAW query then delete that column name from the bucket
 * that contains the data. Thus freeing up the uuid to be queried by user name.
 *
 * THIS FIX SHOULD ONLY BE APPLIED IF PROBLEMS MATCH THESE OTHER CONDITIONS:
 *
 * When running the audit tools if you see two different uuid's for the same entity AND you cannot log in with email.
 * If the unique audit found that this email should be working but the entity_index audit found it to be broken.
 * IF you can PUT on the entity_unique uuid to update properties but CANNOT put on entity_index uuid.
 *
 * The above three conditions should be satisfied before this is ran on ANY user.
 *
 * @author grey
 */
public class ManagementUserIndexMissingFix extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( ManagementUserIndexMissingFix.class );

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
                searchEntityIndex( em, extractedEmail, line );
            }
        }
        else {
            if ( cols.size() == 0 ) {
                logger.error( "Email: {} doesn't exist in ENTITY_UNIQUE.", extractedEmail );
            }
            else {
                logger.error( "Email: {} has {} number of duplicate columns in ENTITY_UNIQUE", extractedEmail,
                        cols.size() );
                UUID uuid = null;
                for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
                    uuid = ue.fromByteBuffer( col.getName() );
                    Entity entity = em.get( uuid );
                    if ( entity == null ) {
                        logger.error( "Email: {} with duplicate uuid: {} doesn't exist in ENTITY_PROPERTIES.",
                                extractedEmail, uuid );
                    }
                    else {
                        Object[] loggerObject = new Object[3];
                        loggerObject[0] = extractedEmail;
                        loggerObject[1] = uuid;
                        loggerObject[2] = entity;
                        logger.info( "Email: {}  with duplicate uuid: {} with the following data: {} exists in "
                                + "ENTITY_PROPERTIES", extractedEmail, uuid );
                    }
                }
            }
        }
    }


    private void searchEntityIndex( final EntityManager em, final String extractedEmail, CommandLine line )
            throws Exception {

        Keyspace ko = cass.getApplicationKeyspace( MANAGEMENT_APPLICATION_ID );
        Mutator<ByteBuffer> m = createMutator( ko, be );

        Query query = new Query();
        query.setEntityType( "user" );
        query.addEqualityFilter( "email", extractedEmail );
        query.setLimit( 1 );
        query.setResultsLevel( REFS );

        RelationManagerImpl relationManager =
                ( RelationManagerImpl ) em.getRelationManager( ref( MANAGEMENT_APPLICATION_ID ) );

        Results r = relationManager.searchCollection( "users", query );
        if ( r != null && r.getRef() != null ) {
            if ( em.get( r.getRef().getUuid() ) == null ) {

                logger.info( "Trying to remove uuid: {} from ENTITY_INDEX.", r.getRef().getUuid() );


                //This method didn't exist before and requires exposing the buffer with the ScanColumn information
                //in the query processor. Hacky, but better than other methods.
                List<ScanColumn> entityIds = relationManager.searchRawCollection( "users", query );

                for ( ScanColumn entityId : entityIds ) {
                    SecondaryIndexSliceParser.SecondaryIndexColumn secondaryIndexColumn =
                            ( SecondaryIndexSliceParser.SecondaryIndexColumn ) entityId;

                    //byte buffer from the scan column is the column name needed to delete from ENTITY_INDEX
                    DynamicComposite columnName = dce.fromByteBuffer( secondaryIndexColumn.getByteBuffer() );
                    String bucketId =
                            ( ( EntityManagerImpl ) em ).getIndexBucketLocator().getBucket( r.getRef().getUuid() );
                    Object index_name = key( MANAGEMENT_APPLICATION_ID, "users", "email" );


                    //only delete from entity_index. Other data might contain valid references which is why
                    //preconditions should be satisfied before trying this method. Preconditions satisfy that
                    //uuid doesn't have valid data anywhere else but in INDEX for specific email. May still have the
                    //broken uuid else where but not of concern to users.

                    Object index_key = key( index_name, bucketId );
                    logger.info( "Deleting the following rowkey: {} from ENTITY_INDEX.", index_key );
                    addDeleteToMutator( m, ENTITY_INDEX, index_key, columnName, createTimestamp() );

                    m.execute();
                }

                Results secondResults = relationManager.searchCollection( "users", query );
                if ( secondResults != null && secondResults.getRef() != null ) {
                    if ( secondResults.getRef().getUuid().equals( r.getRef().getUuid() ) ) {
                        logger.error( "Removing uuid: {} from ENTITY_INDEX did not work. Email: {} still broken.",
                                r.getRef().getUuid(), extractedEmail );
                    }
                }
                else {
                    logger.info( "Delete of uuid: {} from ENTITY_INDEX worked. Email: {} should work.",
                            r.getRef().getUuid(), extractedEmail );
                }
            }
            else {
                logger.error( "Uuid: {} returns a valid entity for email: {} in ENTITY_INDEX.", r.getRef().getUuid(),
                        extractedEmail );
            }
        }

        else {
            logger.error( "Email: {} doesn't exist in ENTITY_INDEX.", extractedEmail );
        }
    }
}
