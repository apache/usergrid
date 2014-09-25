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

import org.apache.usergrid.persistence.core.consistency.TimeService;
import org.apache.usergrid.persistence.core.consistency.TimeServiceImpl;
import org.apache.usergrid.persistence.core.migration.Migration;
import org.apache.usergrid.persistence.core.task.NamedTaskExecutorImpl;
import org.apache.usergrid.persistence.core.task.TaskExecutor;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.impl.GraphManagerImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteListener;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteListenerImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepairImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepairImpl;
import org.apache.usergrid.persistence.graph.impl.stage.NodeDeleteListener;
import org.apache.usergrid.persistence.graph.impl.stage.NodeDeleteListenerImpl;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.NodeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.EdgeShardStrategy;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardAllocation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardApproximation;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.NodeShardCache;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardGroupCompaction;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.ShardedEdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.count.NodeShardApproximationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.count.NodeShardCounterSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.count.NodeShardCounterSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.EdgeShardSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardAllocationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.NodeShardCacheImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.ShardGroupCompactionImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.ShardedEdgeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.SizebasedEdgeColumnFamilies;
import org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.SizebasedEdgeShardStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;


public class GraphModule extends AbstractModule {

    @Override
    protected void configure() {

        //install our configuration
        install( new GuicyFigModule( GraphFig.class ) );


        bind( EdgeMetadataSerialization.class ).to( EdgeMetadataSerializationImpl.class );
        bind( NodeSerialization.class ).to( NodeSerializationImpl.class );

        bind( TimeService.class ).to( TimeServiceImpl.class );

        // create a guice factory for getting our collection manager
        install( new FactoryModuleBuilder().implement( GraphManager.class, GraphManagerImpl.class )
                                           .build( GraphManagerFactory.class ) );


        /**
         * bindings for shard allocations
         */

        bind( NodeShardAllocation.class ).to( NodeShardAllocationImpl.class );
        bind( NodeShardApproximation.class ).to( NodeShardApproximationImpl.class );
        bind( NodeShardCache.class ).to( NodeShardCacheImpl.class );
        bind( NodeShardCounterSerialization.class ).to( NodeShardCounterSerializationImpl.class );

        /**
         * Bind our strategies based on their internal annotations.
         */

        bind( EdgeShardSerialization.class ).to( EdgeShardSerializationImpl.class );


        //Repair/cleanup classes.
        bind( EdgeMetaRepair.class ).to( EdgeMetaRepairImpl.class );
        bind( EdgeDeleteRepair.class ).to( EdgeDeleteRepairImpl.class );


        /**
         * Add our listeners
         */
        bind( NodeDeleteListener.class ).to( NodeDeleteListenerImpl.class );
        bind( EdgeDeleteListener.class ).to( EdgeDeleteListenerImpl.class );

        bind( EdgeSerialization.class ).to( EdgeSerializationImpl.class );

        bind( EdgeShardStrategy.class ).to( SizebasedEdgeShardStrategy.class );

        bind( ShardedEdgeSerialization.class ).to( ShardedEdgeSerializationImpl.class );

        bind( EdgeColumnFamilies.class ).to( SizebasedEdgeColumnFamilies.class );

        bind( ShardGroupCompaction.class).to( ShardGroupCompactionImpl.class);


        /**
         * Bind our implementation
         */

        /********
         * Migration bindings
         ********/


        //do multibindings for migrations
        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
        migrationBinding.addBinding().to( Key.get( NodeSerialization.class ) );
        migrationBinding.addBinding().to( Key.get( EdgeMetadataSerialization.class ) );

        //bind each singleton to the multi set.  Otherwise we won't migrate properly
        migrationBinding.addBinding().to( Key.get( EdgeColumnFamilies.class ) );

        migrationBinding.addBinding().to( Key.get( EdgeShardSerialization.class ) );
        migrationBinding.addBinding().to( Key.get( NodeShardCounterSerialization.class ) );
    }


    @Inject
    @Singleton
    @Provides
    public TaskExecutor graphTaskExecutor(final GraphFig graphFig){
        return new NamedTaskExecutorImpl( "graphTaskExecutor",  graphFig.getShardAuditWorkerCount()  );
    }


}



