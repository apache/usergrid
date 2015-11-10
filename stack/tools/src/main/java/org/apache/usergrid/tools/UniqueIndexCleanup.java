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
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.thrift.TBaseHelper;

import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;
import org.apache.usergrid.utils.StringUtils;
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
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;


/**
 * This is a utility to audit all available entity ids in the secondary index. It then checks to see if any index value
 * is not present in the Entity_Index_Entries. If it is not, the value from the index is removed, and a forced re-index
 * is triggered <p/> USERGRID-323 <p/> <p/> UniqueIndexCleanup -app [appid] -col [collectionname]
 *
 * @author tnine
 */
public class UniqueIndexCleanup extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( UniqueIndexCleanup.class );

    private static final String APPLICATION_ARG = "app";

    private static final String COLLECTION_ARG = "col";

    private static final String ENTITY_UNIQUE_PROPERTY_NAME = "property";

    private static final String ENTITY_UNIQUE_PROPERTY_VALUE = "value";


    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {


        Options options = new Options();

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        options.addOption( hostOption );


        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( false )
                                        .withDescription( "application id" ).create( APPLICATION_ARG );


        options.addOption( appOption );

        Option collectionOption = OptionBuilder.withArgName( COLLECTION_ARG ).hasArg().isRequired( false )
                                               .withDescription( "collection name" ).create( COLLECTION_ARG );

        options.addOption( collectionOption );

        Option entityUniquePropertyName =
                OptionBuilder.withArgName( ENTITY_UNIQUE_PROPERTY_NAME ).hasArg().isRequired( false )
                             .withDescription( "Entity Unique Property Name" ).create( ENTITY_UNIQUE_PROPERTY_NAME );
        options.addOption( entityUniquePropertyName );

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

        if ( line.hasOption( APPLICATION_ARG ) || line.hasOption( COLLECTION_ARG ) ||
                line.hasOption( ENTITY_UNIQUE_PROPERTY_NAME ) || line.hasOption( ENTITY_UNIQUE_PROPERTY_VALUE ) ) {
            deleteInvalidValuesForUniqueProperty( m, line );
        }
        else {

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
                if ( returnedRowKey.contains( "users" ) ) {

                    String[] parsedRowKey = returnedRowKey.split( ":" );

                    //if the rowkey contains more than 4 parts then it may have some garbage appended to the front.
                    if ( parsedRowKey.length > 4 ) {
                        parsedRowKey = garbageRowKeyParser( parsedRowKey );

                        if ( parsedRowKey == null ) {
                            System.out.println( returnedRowKey + " is a invalid row key, and unparseable. Skipped..." );
                            continue;
                        }
                    }
                    //if the rowkey contains less than four parts then it is completely invalid
                    else if ( parsedRowKey.length < 4 ) {
                        System.out.println( returnedRowKey + " is a invalid row key and will be skipped" );
                        continue;
                    }

                    UUID applicationId = null;
                    try {
                        applicationId = UUID.fromString( uuidGarbageParser( parsedRowKey[0] ) );
                    }
                    catch ( Exception e ) {
                        continue;
                    }
                    String collectionName = parsedRowKey[1];
                    String uniqueValueKey = parsedRowKey[2];
                    String uniqueValue = parsedRowKey[3];


                    if ( collectionName.equals( "users" ) ) {

                        ColumnSlice<ByteBuffer, ByteBuffer> columnSlice = rangeSliceValue.getColumnSlice();
                        if ( columnSlice.getColumns().size() != 0 ) {
                            List<HColumn<ByteBuffer, ByteBuffer>> cols = columnSlice.getColumns();
                            if ( cols.size() == 0 ) {
                                deleteRow( m, applicationId, collectionName, uniqueValueKey, uniqueValue );
                            }
                            else {
                                entityUUIDDelete( m, applicationId, collectionName, uniqueValueKey, uniqueValue, cols );
                            }
                        }
                    }
                }
            }
        }
        System.out.println( "Completed repair." );

        logger.info( "Completed audit of apps" );
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
        System.out.println( "Found 0 uuid's associated with: " + uniqueValue );
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
                                   final List<HColumn<ByteBuffer, ByteBuffer>> cols ) throws Exception {
        Boolean cleanup = false;
        EntityManagerImpl em = ( EntityManagerImpl ) emf.getEntityManager( applicationId );
        int numberOfColumnsDeleted = 0;
        //these columns all come from the same row key, which means they each belong to the same row key identifier
        //thus mixing and matching them in the below if cases won't matter.
        Entity[] entities = new Entity[cols.size()];

        if(cols.size() < 2){
            entities = new Entity[2];
        }

        int index = 0;

        for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
            UUID entityId = ue.fromByteBuffer( col.getName() );

            if ( applicationId.equals( MANAGEMENT_APPLICATION_ID ) ) {

                entities[index] = managementService.getAdminUserEntityByUuid( entityId );
                if ( entities[index] == null ) {
                    cleanup = true;
                }
                else {
                    index++;
                }
            }
            else {
                entities[index] = em.get( entityId );
                if ( entities[index] == null ) {
                    cleanup = true;
                }
                else {
                    index++;
                }
            }

            if ( cleanup == true ) {
                numberOfColumnsDeleted++;
                deleteUniqueValue( m, applicationId, collectionName, uniqueValueKey, uniqueValue, entityId );
                cleanup = false;
            }
        }

        //this means that the same unique rowkey has two values associated with it
        if(entities[0]!=null && entities[1]!=null){
            Entity mostRecentEntity = entities[0];
            for(Entity entity: entities){
                if(mostRecentEntity.getModified() > entity.getModified()){
                    em.deleteEntity( entity.getUuid() );
                }
                else if (mostRecentEntity.getModified() < entity.getModified()){
                    em.deleteEntity( mostRecentEntity.getUuid() );
                    mostRecentEntity = entity;
                }
                else if (mostRecentEntity.getModified() == entity.getModified() && !mostRecentEntity.getUuid().equals( entity.getUuid() )){
                    System.out.println("Entities with unique value: "+uniqueValue+" has two or more entities with the same modified time."
                            + "Please manually resolve by query or changing names. ");
                }
            }
        }



        //a safer way to do this would be to try to do another get and verify there is nothing left in the column
        //instead of just doing a simple check since the column check happens anywhere between 2 to 1000 times.
        if ( cols.size() == numberOfColumnsDeleted ) {
            deleteRow( m, applicationId, collectionName, uniqueValueKey, uniqueValue );
        }
    }


    //really only deletes ones that aren't existant for a specific value
    private void deleteInvalidValuesForUniqueProperty( Mutator<ByteBuffer> m, CommandLine line ) throws Exception {
        UUID applicationId = UUID.fromString( line.getOptionValue( APPLICATION_ARG ) );
        String collectionName = line.getOptionValue( COLLECTION_ARG );
        String uniqueValueKey = line.getOptionValue( ENTITY_UNIQUE_PROPERTY_NAME );
        String uniqueValue = line.getOptionValue( ENTITY_UNIQUE_PROPERTY_VALUE );

        Object key = key( applicationId, collectionName, uniqueValueKey, uniqueValue );


        List<HColumn<ByteBuffer, ByteBuffer>> cols =
                cass.getColumns( cass.getApplicationKeyspace( applicationId ), ENTITY_UNIQUE, key, null, null, 1000,
                        false );


        if ( cols.size() == 0 ) {
            System.out.println(
                    "Zero entities were found for this unique value. Its possible it doesn't exist or you typed in in"
                            + " wrong :p." );
        }

        entityUUIDDelete( m, applicationId, collectionName, uniqueValueKey, uniqueValue, cols );
    }


    private String uuidGarbageParser( final String garbageString ) {
        int index = 1;
        String stringToBeTruncated = garbageString;
        while ( !UUIDUtils.isUUID( stringToBeTruncated ) ) {
            if ( stringToBeTruncated.length() > 36 ) {
                stringToBeTruncated = stringToBeTruncated.substring( index );
            }
            else {
                System.out.println( garbageString + " is unparsable" );
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


    private void deleteUniqueValue( final Mutator<ByteBuffer> m, final UUID applicationId, final String collectionName,
                                    final String uniqueValueKey, final String uniqueValue, final UUID entityId )
            throws Exception {
        logger.warn( "Entity with id {} did not exist in app {}", entityId, applicationId );
        System.out.println( "Deleting column uuid: " + entityId.toString() );
        UUID timestampUuid = newTimeUUID();
        long timestamp = getTimestampInMicros( timestampUuid );
        Keyspace ko = cass.getApplicationKeyspace( applicationId );
        Mutator<ByteBuffer> mutator = createMutator( ko, be );

        Object key = key( applicationId, collectionName, uniqueValueKey, uniqueValue );
        addDeleteToMutator( mutator, ENTITY_UNIQUE, key, entityId, timestamp );
        mutator.execute();
        return;
    }
}
