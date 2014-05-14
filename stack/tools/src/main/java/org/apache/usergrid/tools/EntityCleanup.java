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
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.IndexBucketLocator.IndexType;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.EntityManagerImpl;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.query.ir.result.ScanColumn;
import org.apache.usergrid.persistence.query.ir.result.ScanColumnTransformer;
import org.apache.usergrid.persistence.query.ir.result.SliceIterator;
import org.apache.usergrid.persistence.query.ir.result.UUIDIndexSliceParser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.apache.usergrid.persistence.cassandra.ApplicationCF.ENTITY_ID_SETS;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.apache.usergrid.utils.UUIDUtils.newTimeUUID;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;


/**
 * This is a utility to audit all available entity ids for existing target rows If an entity 
 * Id exists in the collection index with no target entity, the id is removed from the index. 
 * This is a cleanup tool as a result of the issue in USERGRID-323
 *
 * @author tnine
 */
public class EntityCleanup extends ToolBase {

    /**
     *
     */
    private static final int PAGE_SIZE = 100;


    private static final Logger logger = LoggerFactory.getLogger( EntityCleanup.class );


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        Options options = new Options();
        options.addOption( hostOption );

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

        Results results = null;


        for ( Entry<String, UUID> app : emf.getApplications().entrySet() ) {

            logger.info( "Starting cleanup for app {}", app.getKey() );

            UUID applicationId = app.getValue();
            EntityManagerImpl em = ( EntityManagerImpl ) emf.getEntityManager( applicationId );

            CassandraService cass = em.getCass();
            IndexBucketLocator indexBucketLocator = em.getIndexBucketLocator();

            UUID timestampUuid = newTimeUUID();
            long timestamp = getTimestampInMicros( timestampUuid );

            Set<String> collectionNames = em.getApplicationCollections();

            // go through each collection and audit the value
            for ( String collectionName : collectionNames ) {

                String type = Schema.getAssociatedEntityType(collectionName);

                IndexScanner scanner = cass.getIdList( cass.getApplicationKeyspace( applicationId ),
                        key( applicationId, DICTIONARY_COLLECTIONS, collectionName ), null, null, PAGE_SIZE, false,
                        indexBucketLocator, applicationId, collectionName, false );

                SliceIterator itr = new SliceIterator( null, scanner, new UUIDIndexSliceParser() );

                while ( itr.hasNext() ) {

                    // load all entity ids from the index itself.

                    Set<ScanColumn> copy = new LinkedHashSet<ScanColumn>( itr.next() );

                    results = em.getEntities(ScanColumnTransformer.getIds( copy ), type );
                    // nothing to do they're the same size so there's no
                    // orphaned uuid's in the entity index
                    if ( copy.size() == results.size() ) {
                        continue;
                    }

                    // they're not the same, we have some orphaned records,
                    // remove them

                    for ( Entity returned : results.getEntities() ) {
                        copy.remove( returned.getUuid() );
                    }

                    // what's left needs deleted, do so

                    logger.info( "Cleaning up {} orphaned entities for app {}", copy.size(), app.getValue() );

                    Keyspace ko = cass.getApplicationKeyspace( applicationId );
                    Mutator<ByteBuffer> m = createMutator( ko, be );

                    for ( ScanColumn col : copy ) {

                        final UUID id = col.getUUID();

                        Object collections_key = key( applicationId, Schema.DICTIONARY_COLLECTIONS, collectionName,
                                indexBucketLocator
                                        .getBucket( applicationId, IndexType.COLLECTION, id, collectionName ) );

                        addDeleteToMutator( m, ENTITY_ID_SETS, collections_key, id, timestamp );

                        logger.info( "Deleting entity with id '{}' from collection '{}'", id, collectionName );
                    }

                    m.execute();
                }
            }
        }
    }
}
