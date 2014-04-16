/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.graph.guice;


import org.safehaus.guicyfig.GuicyFigModule;

import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.event.PostProcessObserver;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessorImpl;
import org.apache.usergrid.persistence.graph.consistency.LocalTimeoutQueue;
import org.apache.usergrid.persistence.graph.consistency.TimeoutQueue;
import org.apache.usergrid.persistence.graph.impl.CollectionIndexObserver;
import org.apache.usergrid.persistence.graph.impl.GraphManagerImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepairImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepairImpl;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.CommitLog;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.PermanentStorage;
import org.apache.usergrid.persistence.graph.serialization.impl.CassandraConfigImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.NodeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeSeriesCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeSeriesSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardCache;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.EdgeSeriesCounterSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.EdgeSeriesSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardAllocationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardApproximationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardCacheImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.SizebasedEdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.TimebasedEdgeShardStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.netflix.astyanax.Keyspace;


public class GraphModule extends AbstractModule {

    @Override
    protected void configure() {

        //configure collections and our core astyanax framework
        install( new CollectionModule() );

        //install our configuration
        install( new GuicyFigModule( GraphFig.class ) );

        bind( PostProcessObserver.class ).to( CollectionIndexObserver.class );

        bind( EdgeMetadataSerialization.class ).to( EdgeMetadataSerializationImpl.class );
        bind( NodeSerialization.class ).to( NodeSerializationImpl.class );


        bind( CassandraConfig.class ).to( CassandraConfigImpl.class );

        // create a guice factory for getting our collection manager
        install( new FactoryModuleBuilder().implement( GraphManager.class, GraphManagerImpl.class )
                                           .build( GraphManagerFactory.class ) );


        //do multibindings for migrations
        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().to( EdgeMetadataSerializationImpl.class );
        migrationBinding.addBinding().to( NodeSerializationImpl.class );


        /**
         * bindings for shard allocations
         */

        bind( NodeShardAllocation.class ).to( NodeShardAllocationImpl.class );
        bind( NodeShardApproximation.class ).to( NodeShardApproximationImpl.class );
        bind( NodeShardCache.class ).to( NodeShardCacheImpl.class );

        /**
         * Bind our strategies based on their internal annotations.
         */

        bind( EdgeSeriesSerialization.class ).to( EdgeSeriesSerializationImpl.class );
        bind( EdgeSeriesCounterSerialization.class ).to( EdgeSeriesCounterSerializationImpl.class );


        /**
         * Graph event bus, will need to be refactored into it's own classes
         */

        // create a guice factory for getting our collection manager

        //local queue.  Need to replace with a real implementation
        bind( TimeoutQueue.class ).to( LocalTimeoutQueue.class );

        bind( AsyncProcessor.class ).annotatedWith( EdgeDelete.class ).to( AsyncProcessorImpl.class );
        bind( AsyncProcessor.class ).annotatedWith( NodeDelete.class ).to( AsyncProcessorImpl.class );

        //Repair/cleanup classes
        bind( EdgeMetaRepair.class ).to( EdgeMetaRepairImpl.class );


        bind( EdgeDeleteRepair.class ).to( EdgeDeleteRepairImpl.class );
    }


    /**
     * Our permanent serialization strategy
     */
    @Provides
    @Singleton
    @PermanentStorage
    @Inject
    public EdgeSerialization permanentStorageSerialization( final NodeShardCache cache, final Keyspace keyspace,
                                                            final CassandraConfig cassandraConfig,
                                                            final GraphFig graphFig) {

        final EdgeShardStrategy sizeBasedStrategy = new SizebasedEdgeShardStrategy( cache );

        final EdgeSerializationImpl edgeSerialization =
                new EdgeSerializationImpl( keyspace, cassandraConfig, graphFig, sizeBasedStrategy );


        //register this instance in the multi binding
        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().toInstance( edgeSerialization );


        return edgeSerialization;
    }


    /**
     * The commit log strategy for fast writes
     */
    @Provides
    @Singleton
    @CommitLog
    @Inject
    public EdgeSerialization commitlogStorageSerialization( final NodeShardCache cache, final Keyspace keyspace,
                                                            final CassandraConfig cassandraConfig,
                                                            final GraphFig graphFig) {

        final EdgeShardStrategy sizeBasedStrategy = new TimebasedEdgeShardStrategy( cache );

        final EdgeSerializationImpl edgeSerialization =
                new EdgeSerializationImpl( keyspace, cassandraConfig, graphFig, sizeBasedStrategy );


        //register this instance in the multi binding
        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().toInstance( edgeSerialization );


        return edgeSerialization;
    }
}



