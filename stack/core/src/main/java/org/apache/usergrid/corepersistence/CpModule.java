/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.usergrid.corepersistence;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import org.apache.usergrid.persistence.collection.guice.CollectionModule;
import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.event.PostProcessObserver;
import org.apache.usergrid.persistence.graph.EdgeManager;
import org.apache.usergrid.persistence.graph.EdgeManagerFactory;
import org.apache.usergrid.persistence.graph.GraphFig;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.graph.consistency.AsyncProcessorImpl;
import org.apache.usergrid.persistence.graph.consistency.LocalTimeoutQueue;
import org.apache.usergrid.persistence.graph.consistency.TimeService;
import org.apache.usergrid.persistence.graph.consistency.TimeoutQueue;
import org.apache.usergrid.persistence.graph.guice.EdgeDelete;
import org.apache.usergrid.persistence.graph.guice.EdgeWrite;
import org.apache.usergrid.persistence.graph.guice.NodeDelete;
import org.apache.usergrid.persistence.graph.impl.CollectionIndexObserver;
import org.apache.usergrid.persistence.graph.impl.EdgeManagerImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeDeleteRepairImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeMetaRepairImpl;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeWriteRepair;
import org.apache.usergrid.persistence.graph.impl.stage.EdgeWriteRepairImpl;
import org.apache.usergrid.persistence.graph.serialization.CassandraConfig;
import org.apache.usergrid.persistence.graph.serialization.EdgeMetadataSerialization;
import org.apache.usergrid.persistence.graph.serialization.EdgeSerialization;
import org.apache.usergrid.persistence.graph.serialization.NodeSerialization;
import org.apache.usergrid.persistence.graph.serialization.impl.CassandraConfigImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeMetadataSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.EdgeSerializationImpl;
import org.apache.usergrid.persistence.graph.serialization.impl.NodeSerializationImpl;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.impl.EsEntityCollectionIndex;
import org.safehaus.guicyfig.GuicyFigModule;

/**
 * Guice Module that encapsulates Core Persistence.
 */
public class CpModule  extends AbstractModule {

    @Override
    protected void configure() {

        //------------
        // COLLECTION
        //

        // configure collections and our core astyanax framework
        install(new CollectionModule());

        //------------
        // INDEX 
        //

        install (new GuicyFigModule( IndexFig.class ));

        install( new FactoryModuleBuilder()
            .implement( EntityCollectionIndex.class, EsEntityCollectionIndex.class )
            .build( EntityCollectionIndexFactory.class ) );

        //------------
        // GRAPH 
        //

        install (new GuicyFigModule( GraphFig.class ));
        
        bind( PostProcessObserver.class ).to( CollectionIndexObserver.class );

        bind( EdgeMetadataSerialization.class).to( EdgeMetadataSerializationImpl.class);
        bind( EdgeSerialization.class).to( EdgeSerializationImpl.class );
        bind( NodeSerialization.class).to( NodeSerializationImpl.class );

        bind( CassandraConfig.class).to( CassandraConfigImpl.class );

         // create a guice factory for getting our collection manager
        install( new FactoryModuleBuilder()
                .implement( EdgeManager.class, EdgeManagerImpl.class )
                .build( EdgeManagerFactory.class ) );
        
        Multibinder<Migration> migrationBinding = 
            Multibinder.newSetBinder( binder(), Migration.class );

        migrationBinding.addBinding().to( EdgeMetadataSerializationImpl.class );
        migrationBinding.addBinding().to( EdgeSerializationImpl.class );
        migrationBinding.addBinding().to( NodeSerializationImpl.class );

        // local queue
        bind(TimeoutQueue.class).to( LocalTimeoutQueue.class );


        bind( AsyncProcessor.class).annotatedWith( EdgeDelete.class ).to( AsyncProcessorImpl.class );
        bind( AsyncProcessor.class).annotatedWith( EdgeWrite.class ).to( AsyncProcessorImpl.class );
        bind( AsyncProcessor.class).annotatedWith( NodeDelete.class ).to( AsyncProcessorImpl.class );

        // Repair/cleanup classes
        bind( EdgeMetaRepair.class).to( EdgeMetaRepairImpl.class );
        bind( EdgeWriteRepair.class).to( EdgeWriteRepairImpl.class );
        bind( EdgeDeleteRepair.class).to( EdgeDeleteRepairImpl.class );

        bind( TimeService.class).to( TimeServiceImpl.class );
    }    
}
