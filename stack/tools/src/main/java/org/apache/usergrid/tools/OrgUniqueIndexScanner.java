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


import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.TimedOutException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.thrift.TBaseHelper;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.PagingResultsIterator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;
import org.apache.usergrid.persistence.cassandra.RelationManagerImpl;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.query.ir.result.ScanColumn;
import org.apache.usergrid.persistence.query.ir.result.UUIDColumn;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.cassandra.service.RangeSlicesIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.RangeSlicesQuery;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.SimpleEntityRef.ref;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;


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
public class OrgUniqueIndexScanner extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( OrgUniqueIndexScanner.class );

    private static final String ENTITY_UNIQUE_PROPERTY_VALUE = "value";


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = new Options();

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        options.addOption( hostOption );

        Option entityUniquePropertyValue =
                OptionBuilder.withArgName( ENTITY_UNIQUE_PROPERTY_VALUE ).hasArg().isRequired( false )
                             .withDescription( "Entity Unique Property Value" ).create( ENTITY_UNIQUE_PROPERTY_VALUE );
        options.addOption( entityUniquePropertyValue );


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

        logger.info( "Starting entity unique index cleanup" );


        // go through each collection and audit the values
        Keyspace ko = cass.getUsergridApplicationKeyspace();
        Mutator<ByteBuffer> m = createMutator( ko, be );

//        EntityManagerImpl em = ( EntityManagerImpl ) emf.getEntityManager( MANAGEMENT_APPLICATION_ID );
//
//        RelationManagerImpl relationManager =
//                ( RelationManagerImpl ) em.getRelationManager( ref( MANAGEMENT_APPLICATION_ID ) );
//
//        Query query = new Query();
//        query.setEntityType( "group" );
//        query.setLimit( MAX_ENTITY_FETCH );
//        query.setResultsLevel( Results.Level.REFS );
//
//        Results r = relationManager.searchCollection( "groups",query );

        HashSet<UUID> orgUUIDS = getOrgUUIDS();
        int index = 0;


        if ( line.hasOption( ENTITY_UNIQUE_PROPERTY_VALUE ) ) {
            deleteInvalidValuesForUniqueProperty( m, line,orgUUIDS );
        }
        else {


            //maybe put a byte buffer infront.
            RangeSlicesQuery<ByteBuffer, ByteBuffer, ByteBuffer> rangeSlicesQuery =
                    HFactory.createRangeSlicesQuery( ko, be, be, be ).setColumnFamily( ENTITY_UNIQUE.getColumnFamily() )
                            //not sure if I trust the lower two settings as it might iterfere with paging or set
                            // arbitrary limits and what I want to retrieve.
                            //That needs to be verified.
                            .setKeys( null, null ).setRange( null, null, false, PAGE_SIZE );


            RangeSlicesIterator rangeSlicesIterator = new RangeSlicesIterator( rangeSlicesQuery, null, null );

            while ( rangeSlicesIterator.hasNext() ) {
                Row rangeSliceValue = rangeSlicesIterator.next();


                ByteBuffer buf = ( TBaseHelper.rightSize( ( ByteBuffer ) rangeSliceValue.getKey() ) );
                //Cassandra client library returns ByteBuffers that are views on top of a larger byte[]. These larger
                // ones return garbage data.
                //Discovered thanks due to https://issues.apache.org/jira/browse/NUTCH-1591
                String returnedRowKey = new String( buf.array(), buf.arrayOffset() + buf.position(), buf.remaining(),
                        Charset.defaultCharset() ).trim();


                //defensive programming, don't have to have to parse the string if it doesn't contain users.
                if ( returnedRowKey.contains( "groups" ) && returnedRowKey
                        .contains( MANAGEMENT_APPLICATION_ID.toString() ) ) {
                    String[] parsedRowKey = returnedRowKey.split( ":" );

                    //if the rowkey contains more than 4 parts then it may have some garbage appended to the front.
                    if ( parsedRowKey.length > 4 ) {
                        parsedRowKey = garbageRowKeyParser( parsedRowKey );

                        if ( parsedRowKey == null ) {
                            logger.error( "{} is a invalid row key, and unparseable. Skipped...", returnedRowKey );
                            continue;
                        }
                    }
                    //if the rowkey contains less than four parts then it is completely invalid
                    else if ( parsedRowKey.length < 4 ) {
                        logger.error( "{} is a invalid row key, and unparseable. Skipped...", returnedRowKey );
                        continue;
                    }

                    UUID applicationId = null;
                    try {
                        applicationId = UUID.fromString( uuidGarbageParser( parsedRowKey[0] ) );
                    }
                    catch ( Exception e ) {
                        logger.error( "could not parse {} despite earlier parsing. Skipping...", parsedRowKey[0] );
                        continue;
                    }
                    String collectionName = parsedRowKey[1];
                    String uniqueValueKey = parsedRowKey[2];
                    String uniqueValue = parsedRowKey[3];


                    //if ( collectionName.equals( "users" ) ) {

                    ColumnSlice<ByteBuffer, ByteBuffer> columnSlice = rangeSliceValue.getColumnSlice();
                    //if ( columnSlice.getColumns().size() != 0 ) {
                    List<HColumn<ByteBuffer, ByteBuffer>> cols = columnSlice.getColumns();


                    entityUUIDDelete( m, applicationId, collectionName, uniqueValueKey, uniqueValue, cols,
                            returnedRowKey,orgUUIDS);
                    index++;
               // }
                // }
                //}
            }
        }
    }


    logger.info("Completed repair successfully.Scanned over {} number of orgs",index);
}



    //Returns a functioning rowkey if it can otherwise returns null
    public String[] garbageRowKeyParser( String[] parsedRowKey ) {
        String[] modifiedRowKey = parsedRowKey.clone();
        while ( modifiedRowKey != null ) {
            if ( modifiedRowKey.length < 4 ) {
                return null;
            }

            String recreatedRowKey = uuidStringVerifier( modifiedRowKey[0] );
            if ( recreatedRowKey == null ) {
                recreatedRowKey = "";
                modifiedRowKey = getStrings( modifiedRowKey, recreatedRowKey );
            }
            else {
                recreatedRowKey = recreatedRowKey.concat( ":" );
                modifiedRowKey = getStrings( modifiedRowKey, recreatedRowKey );
                break;
            }
        }
        return modifiedRowKey;
    }


    private String[] getStrings( String[] modifiedRowKey, String recreatedRowKey ) {
        for ( int i = 1; i < modifiedRowKey.length; i++ ) {

            recreatedRowKey = recreatedRowKey.concat( modifiedRowKey[i] );
            if ( i + 1 != modifiedRowKey.length ) {
                recreatedRowKey = recreatedRowKey.concat( ":" );
            }
        }
        modifiedRowKey = recreatedRowKey.split( ":" );
        return modifiedRowKey;
    }


    private void deleteRow( final Mutator<ByteBuffer> m, final UUID applicationId, final String collectionName,
                            final String uniqueValueKey, final String uniqueValue ) throws Exception {
        logger.debug( "Found 0 uuid's associated with {} Deleting row.", uniqueValue );
        UUID timestampUuid = newTimeUUID();
        long timestamp = getTimestampInMicros( timestampUuid );

        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> mutator = createMutator( ko, be );

        Object key = key( applicationId, collectionName, uniqueValueKey, uniqueValue );
        addDeleteToMutator( mutator, ENTITY_UNIQUE, key, timestamp );
        mutator.execute();
        return;
    }


    private void entityUUIDDelete( final Mutator<ByteBuffer> m, final UUID applicationId, final String collectionName,
                                   final String uniqueValueKey, final String uniqueValue,
                                   final List<HColumn<ByteBuffer, ByteBuffer>> cols, String rowKey,HashSet<UUID> orgUUIDS) throws Exception {
        Boolean cleanup = false;
        EntityManagerImpl em = ( EntityManagerImpl ) emf.getEntityManager( MANAGEMENT_APPLICATION_ID );

        int numberOfColumnsDeleted = 0;
        //these columns all come from the same row key, which means they each belong to the same row key identifier
        //thus mixing and matching them in the below if cases won't matter.
        Entity[] entities = new Entity[cols.size()];
        int numberOfRetrys = 8;
        int numberOfTimesRetrying = 0;

        int index = 0;


        if ( cols.size() == 1 ) {
            UUID uuid = null;
            for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
                uuid = ue.fromByteBuffer( col.getName() );
            }

            if ( em.get( uuid ) == null ) {
                logger.error( "OrgName: {} with uuid: {} doesn't exist in ENTITY_PROPERTIES.", uniqueValue, uuid );

                if(orgUUIDS.contains( uuid )){
                    logger.error( "OrgName: {} with uuid: {} doesn't exist in ENTITY_PROPERTIES but exists in ENTITY_INDEX.Repairing...", uniqueValue, uuid );

                    Group organizationEntity = new Group();
                    organizationEntity.setPath( uniqueValue );
                    try {
                        em.create( uuid, Group.ENTITY_TYPE, organizationEntity.getProperties() );
                        organizationEntity = em.get( uuid, Group.class );
                        if ( organizationEntity == null ) {
                            logger.error( "OrgName: {} with uuid: {} could not be created", uniqueValue, uuid );
                        }
                        else{
//                            logger.info( "OrgName: {}  with uuid: {} was fixed.", uniqueValue,
//                                    uuid );
                        }
                    }catch(Exception e){
                        logger.error("Could not create: {} because of {}",uniqueValue,e.getMessage());
                    }

                }
                else{
                    logger.error( "OrgName: {} with uuid: {} doesn't exist in ENTITY_PROPERTIES OR ENTITY INDEX. Leaving alone.", uniqueValue, uuid );

                }
            }
            else {
//                logger.info( "OrgName: {}  with uuid: {} exists in ENTITY_PROPERTIES for ENTITY_UNIQUE.", uniqueValue,
//                        uuid );
            }
        }

    }


    private HashSet<UUID> getOrgUUIDS() throws Exception {
        EntityManager em = emf.getEntityManager( MANAGEMENT_APPLICATION_ID );
        RelationManagerImpl relationManager =
                ( RelationManagerImpl ) em.getRelationManager( ref( MANAGEMENT_APPLICATION_ID ) );
        Query query = new Query();
        query.setLimit( 1000 );
        query.setResultsLevel( Results.Level.IDS );

        HashSet<UUID> orgUUIDs = new HashSet(  );

        Results results = relationManager.searchCollection( "groups",query );

        PagingResultsIterator pagingResultsIterator = new PagingResultsIterator( results );
        while(pagingResultsIterator.hasNext()){
            orgUUIDs.add((UUID) pagingResultsIterator.next());
        }
        logger.info( "Found {} number of orgs",orgUUIDs.size() );
        return orgUUIDs;
    }


    private Entity verifyModifiedTimestamp( final Entity unverifiedEntity ) {
        Entity entity = unverifiedEntity;
        if ( entity != null && entity.getModified() == null ) {
            if ( entity.getCreated() != null ) {
                logger.debug( "{} has no modified. Subsituting created timestamp for their modified timestamp.Manually "
                        + "adding one for comparison purposes", entity.getUuid() );
                entity.setModified( entity.getCreated() );
                return entity;
            }
            else {
                logger.error( "Found no created or modified timestamp. Please remake the following entity: {}."
                        + " Setting both created and modified to 1", entity.getUuid().toString() );
                entity.setCreated( 1L );
                entity.setModified( 1L );
                return entity;
            }
        }
        return entity;
    }


    //really only deletes ones that aren't existant for a specific value
    private void deleteInvalidValuesForUniqueProperty( Mutator<ByteBuffer> m, CommandLine line,
                                                       final HashSet<UUID> orgUUIDS ) throws Exception {
        UUID applicationId = MANAGEMENT_APPLICATION_ID;
        String collectionName = "groups";
        String uniqueValueKey = "path";
        String uniqueValue = line.getOptionValue( ENTITY_UNIQUE_PROPERTY_VALUE);


        //PLEASE ADD VERIFICATION.

        Object key = key(applicationId, collectionName, uniqueValueKey,uniqueValue );


        List<HColumn<ByteBuffer, ByteBuffer>> cols =
                cass.getColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_UNIQUE, key, null, null, 1000,
                        false  );


        if ( cols.size() == 0 ) {
            logger.error( "This row key: {} has zero columns. Leaving alone.", key.toString() );
        }
        else if(cols.size() > 1){
            logger.error( "This row key: {} has duplicate entries columns. Leaving alone.", key.toString() );

        }

//        EntityManagerImpl em = ( EntityManagerImpl ) emf.getEntityManager( applicationId );
//
//        RelationManagerImpl relationManager =
//                ( RelationManagerImpl ) em.getRelationManager( ref( MANAGEMENT_APPLICATION_ID ) );

//        Query query = new Query();
//        query.setEntityType( "group" );
//        query.setLimit( MAX_ENTITY_FETCH );
//        query.setResultsLevel( Results.Level.REFS );
//
//        Results r = relationManager.searchCollection( "groups",query );
//
//        managementService.getOrganizationByUuid(  )
//
//        logger.info("Does this work?: {}",r);


        entityUUIDDelete( m, applicationId, collectionName, uniqueValueKey, uniqueValue, cols, key.toString(),orgUUIDS);
    }


    private String uuidGarbageParser( final String garbageString ) {
        int index = 1;
        String stringToBeTruncated = garbageString;
        while ( !UUIDUtils.isUUID( stringToBeTruncated ) ) {
            if ( stringToBeTruncated.length() > 36 ) {
                stringToBeTruncated = stringToBeTruncated.substring( index );
            }
            else {
                logger.error( "{} is unparsable", garbageString );
                break;
            }
        }
        return stringToBeTruncated;
    }


    private String uuidStringVerifier( final String garbageString ) {
        int index = 1;
        String stringToBeTruncated = garbageString;
        while ( !UUIDUtils.isUUID( stringToBeTruncated ) ) {
            if ( stringToBeTruncated.length() > 36 ) {
                stringToBeTruncated = stringToBeTruncated.substring( index );
            }
            else {
                return null;
            }
        }
        return stringToBeTruncated;
    }
}
