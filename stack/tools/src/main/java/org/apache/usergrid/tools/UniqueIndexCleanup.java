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
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.IndexBucketLocator.IndexType;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.query.ir.result.ScanColumn;
import org.apache.usergrid.persistence.query.ir.result.SliceIterator;
import org.apache.usergrid.persistence.query.ir.result.UUIDIndexSliceParser;
import org.apache.usergrid.persistence.schema.CollectionInfo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.apache.usergrid.persistence.Schema.getDefaultSchema;
import org.apache.usergrid.persistence.SimpleEntityRef;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX_ENTRIES;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.persistence.cassandra.CassandraService.INDEX_ENTRY_LIST_COUNT;
import static org.apache.usergrid.utils.CompositeUtils.setEqualityFlag;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;


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
                                               .withDescription( "colleciton name" ).create( COLLECTION_ARG );

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
            IndexBucketLocator indexBucketLocator = em.getIndexBucketLocator();

            Keyspace ko = cass.getApplicationKeyspace( applicationId );

            UUID timestampUuid = newTimeUUID();
            long timestamp = getTimestampInMicros( timestampUuid );


            // go through each collection and audit the values
            for ( String collectionName : getCollectionNames( em, line ) ) {


                IndexScanner scanner = cass.getIdList( cass.getApplicationKeyspace( applicationId ),
                        key( applicationId, DICTIONARY_COLLECTIONS, collectionName ), null, null, PAGE_SIZE, false,
                        indexBucketLocator, applicationId, collectionName, false );

                SliceIterator itr = new SliceIterator( null, scanner, new UUIDIndexSliceParser() );


                while ( itr.hasNext() ) {

                    Set<ScanColumn> ids = itr.next();

                    CollectionInfo collection = getDefaultSchema().getCollection( "application", collectionName );


                    //We shouldn't have to do this, but otherwise the cursor won't work
                    Set<String> indexed = collection.getPropertiesIndexed();

                    // what's left needs deleted, do so

                    logger.info( "Auditing {} entities for collection {} in app {}", new Object[] {
                            ids.size(), collectionName, app.getValue()
                    } );

                    for ( ScanColumn col : ids ) {
                        final UUID id = col.getUUID();
                        String type = getDefaultSchema().getCollectionType("application", collectionName);

                        boolean reIndex = false;

                        Mutator<ByteBuffer> m = createMutator( ko, be );

                        try {

                            for ( String prop : indexed ) {

                                String bucket =
                                        indexBucketLocator.getBucket( applicationId, IndexType.COLLECTION, id, prop );

                                Object rowKey = key( applicationId, collection.getName(), prop, bucket );

                                List<HColumn<ByteBuffer, ByteBuffer>> indexCols =
                                        scanIndexForAllTypes( ko, indexBucketLocator, applicationId, rowKey, id, prop );

                                // loop through the indexed values and verify them as present in
                                // our entity_index_entries. If they aren't, we need to delete the
                                // from the secondary index, and mark
                                // this object for re-index via n update
                                for ( HColumn<ByteBuffer, ByteBuffer> index : indexCols ) {

                                    DynamicComposite secondaryIndexValue =
                                            DynamicComposite.fromByteBuffer( index.getName().duplicate() );

                                    Object code = secondaryIndexValue.get( 0 );
                                    Object propValue = secondaryIndexValue.get( 1 );
                                    UUID timestampId = ( UUID ) secondaryIndexValue.get( 3 );

                                    DynamicComposite existingEntryStart =
                                            new DynamicComposite( prop, code, propValue, timestampId );
                                    DynamicComposite existingEntryFinish =
                                            new DynamicComposite( prop, code, propValue, timestampId );

                                    setEqualityFlag( existingEntryFinish, ComponentEquality.GREATER_THAN_EQUAL );

                                    // now search our EntityIndexEntry for previous values, see if
                                    // they don't match this one

                                    List<HColumn<ByteBuffer, ByteBuffer>> entries =
                                            cass.getColumns( ko, ENTITY_INDEX_ENTRIES, id, existingEntryStart,
                                                    existingEntryFinish, INDEX_ENTRY_LIST_COUNT, false );

                                    // we wouldn't find this column in our entity_index_entries
                                    // audit. Delete it, then mark this entity for update
                                    if ( entries.size() == 0 ) {
                                        logger.info(
                                            "Could not find reference to value '{}' property '{}'"+
                                            " on entity {} in collection {}. " + " Forcing reindex",
                                            new Object[] { propValue, prop, id, collectionName } );

                                        addDeleteToMutator(
                                            m, ENTITY_INDEX, rowKey, index.getName().duplicate(), timestamp );

                                        reIndex = true;
                                    }

                                    if ( entries.size() > 1 ) {
                                        logger.info(
                                            "Found more than 1 entity referencing unique index "
                                          + "for property '{}' with value " + "'{}'",
                                            prop, propValue );
                                        reIndex = true;
                                    }
                                }
                            }

                            //force this entity to be updated
                            if ( reIndex ) {
                                Entity entity = em.get( new SimpleEntityRef( type, id ));

                                //entity may not exist, but we should have deleted rows from the index
                                if ( entity == null ) {
                                    logger.warn( "Entity with id {} did not exist in app {}",
                                            id, applicationId );
                                    //now execute the cleanup. In this case the entity is gone,
                                    // so we'll want to remove references from
                                    // the secondary index
                                    m.execute();
                                    continue;
                                }


                                logger.info( "Reindex complete for entity with id '{} ", id );
                                em.update( entity );

                                //now execute the cleanup. This way if the above update fails,
                                // we still have enough data to run again
                                // later
                                m.execute();
                            }
                        }
                        catch ( Exception e ) {
                            logger.error( "Unable to process entity with id '{}'", id, e );
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
