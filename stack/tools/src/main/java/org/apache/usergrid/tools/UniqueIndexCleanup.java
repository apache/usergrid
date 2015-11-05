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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.RowIteratorFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.Charsets;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;
import org.apache.usergrid.persistence.cassandra.Serializers;
import org.apache.usergrid.persistence.cassandra.index.IndexBucketScanner;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.cassandra.index.UUIDStartToBytes;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.query.ir.result.ScanColumn;
import org.apache.usergrid.persistence.query.ir.result.SliceIterator;
import org.apache.usergrid.persistence.query.ir.result.UUIDIndexSliceParser;
import org.apache.usergrid.persistence.schema.CollectionInfo;

import me.prettyprint.cassandra.service.KeyIterator;
import me.prettyprint.cassandra.service.RangeSlicesIterator;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.ColumnSlice;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.OrderedRows;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.QueryResult;
import me.prettyprint.hector.api.query.RangeSlicesQuery;
import me.prettyprint.hector.api.query.SliceQuery;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static me.prettyprint.hector.api.factory.HFactory.createRangeSlicesQuery;
import static me.prettyprint.hector.api.factory.HFactory.createSliceQuery;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ID_SETS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_PROPERTIES;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.APPLICATIONS_CF;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.persistence.cassandra.Serializers.dce;
import static org.apache.usergrid.persistence.cassandra.Serializers.le;
import static org.apache.usergrid.persistence.cassandra.Serializers.se;
import static org.apache.usergrid.persistence.cassandra.Serializers.ue;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;


/**
 * This is a utility to audit all available entity ids in the secondary index. It then checks to see if any index value
 * is not present in the Entity_Index_Entries. If it is not, the value from the index is removed, and a forced re-index
 * is triggered
 * <p/>
 * USERGRID-323
 * <p/>
 * <p/>
 * UniqueIndexCleanup -app [appid] -col [collectionname]
 *
 * @author tnine
 */
public class UniqueIndexCleanup extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;



    private static final Logger logger = LoggerFactory.getLogger( UniqueIndexCleanup.class );

    /**
     *
     */
    private static final String APPLICATION_ARG = "app";

    /**
     *
     */
    private static final String COLLECTION_ARG = "col";


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {


        Options options = new Options();

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );


        options.addOption( hostOption );


        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( false )
                                        .withDescription( "application id or app name" ).create( APPLICATION_ARG );


        options.addOption( appOption );

        Option collectionOption = OptionBuilder.withArgName( COLLECTION_ARG ).hasArg().isRequired( false )
                                               .withDescription( "collection name" ).create( COLLECTION_ARG );

        options.addOption( collectionOption );

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

        logger.info( "Starting entity cleanup" );

        Map<String, UUID> apps = getApplications( emf, line );


        for ( Entry<String, UUID> app : apps.entrySet() ) {

            logger.info( "Starting cleanup for app {}", app.getKey() );

            UUID applicationId = app.getValue();
            EntityManagerImpl em = ( EntityManagerImpl ) emf.getEntityManager( applicationId );

            //sanity check for corrupt apps
            Application appEntity = em.getApplication();

            if ( appEntity == null ) {
                logger.warn( "Application does not exist in data. {}", app.getKey() );
                continue;
            }

            CassandraService cass = em.getCass();

            Keyspace ko = cass.getUsergridApplicationKeyspace();
            Mutator<ByteBuffer> m = createMutator( ko, be );


            UUID timestampUuid = newTimeUUID();
            long timestamp = getTimestampInMicros( timestampUuid );


            // go through each collection and audit the values
            for ( String collectionName : getCollectionNames( em, line ) ) {

                RangeSlicesQuery<ByteBuffer, ByteBuffer, ByteBuffer> rangeSlicesQuery = HFactory
                        .createRangeSlicesQuery( ko, be, be, be )
                        .setColumnFamily( ENTITY_UNIQUE.getColumnFamily() )
                        //not sure if I trust the lower two ssettings as it might iterfere with paging or set arbitrary limits and what I want to retrieve.
                        //That needs to be verified.
                        .setKeys( null, null )
                        .setRange( null, null, false, 100 );



                RangeSlicesIterator rangeSlicesIterator = new RangeSlicesIterator( rangeSlicesQuery,null,null );
                QueryResult<OrderedRows<ByteBuffer, ByteBuffer, ByteBuffer>> result = rangeSlicesQuery.execute();
                OrderedRows<ByteBuffer, ByteBuffer, ByteBuffer> rows = result.get();
                result.get().getList().get( 0 ).getColumnSlice();

                while(rangeSlicesIterator.hasNext()) {
                    //UUID returned_uuid = UUID.nameUUIDFromBytes(((ByteBuffer)rangeSlicesIterator.next().getKey()).array());
                    Row rangeSliceValue = rangeSlicesIterator.next();

                    String returnedRowKey =
                            new String( ( ( ByteBuffer ) rangeSliceValue.getKey() ).array(), Charsets.UTF_8 ).trim();

                    String[] parsedRowKey = returnedRowKey.split( ":" );
                    if ( parsedRowKey[1].equals( "users" ) || returnedRowKey.contains( "username" ) || returnedRowKey
                            .contains( "email" ) ) {
                        ColumnSlice<ByteBuffer, ByteBuffer> columnSlice = rangeSliceValue.getColumnSlice();
                        if ( columnSlice.getColumns().size() != 0 ) {
                            System.out.println( returnedRowKey );
                            List<HColumn<ByteBuffer, ByteBuffer>> cols = columnSlice.getColumns();

                            for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
                                UUID entityId = ue.fromByteBuffer( col.getName() );


                                if ( em.get( entityId ) == null && managementService.getAdminUserByUuid( entityId )==null ) {
                                    logger.warn( "Entity with id {} did not exist in app {}", entityId, applicationId );
                                    System.out.println( "Deleting column uuid: " + entityId.toString() );


                                    Object key = key( applicationId, collectionName, "username", entityId );
                                    addDeleteToMutator( m, ENTITY_UNIQUE, key, entityId, timestamp );
                                    m.execute();
                                    continue;
                                }
                            }
                        }
                    }
                }

            }
        }

        logger.info( "Completed audit of apps" );
    }


    private Map<String, UUID> getApplications( EntityManagerFactory emf, CommandLine line ) throws Exception {
        String appName = line.getOptionValue( APPLICATION_ARG );

        if ( appName == null ) {
            return emf.getApplications();
        }

        ApplicationInfo app = managementService.getApplicationInfo( Identifier.from( appName ) );

        if ( app == null ) {
            logger.error( "Could not find application with id or name {}", appName );
            System.exit( 3 );
        }


        Map<String, UUID> apps = new HashMap<String, UUID>();

        apps.put( app.getName(), app.getId() );

        return apps;
    }


    private Set<String> getCollectionNames( EntityManager em, CommandLine line ) throws Exception {

        String collectionName = line.getOptionValue( COLLECTION_ARG );

        if ( collectionName == null ) {
            return em.getApplicationCollections();
        }


        Set<String> names = new HashSet<String>();
        names.add( collectionName );

        return names;
    }


    private List<HColumn<ByteBuffer, ByteBuffer>> scanIndexForAllTypes( Keyspace ko,
                                                                        IndexBucketLocator indexBucketLocator,
                                                                        UUID applicationId, Object rowKey,
                                                                        UUID entityId, String prop ) throws Exception {

        //TODO Determine the index bucket.  Scan the entire index for properties with this entityId.


        DynamicComposite start = null;

        List<HColumn<ByteBuffer, ByteBuffer>> cols;

        List<HColumn<ByteBuffer, ByteBuffer>> results = new ArrayList<HColumn<ByteBuffer, ByteBuffer>>();


        do {
            cols = cass.getColumns( ko, ENTITY_INDEX, rowKey, start, null, 100, false );

            for ( HColumn<ByteBuffer, ByteBuffer> col : cols ) {
                DynamicComposite secondaryIndexValue = DynamicComposite.fromByteBuffer( col.getName().duplicate() );

                UUID storedId = ( UUID ) secondaryIndexValue.get( 2 );

                //add it to the set.  We can't short circuit due to property ordering
                if ( entityId.equals( storedId ) ) {
                    results.add( col );
                }

                start = secondaryIndexValue;
            }
        }
        while ( cols.size() == 100 );

        return results;
    }
}
