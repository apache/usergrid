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
import org.apache.usergrid.persistence.index.guice.IndexModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Guice Module that encapsulates Core Persistence.
 */
public class GuiceModule  extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger( GuiceModule.class );

    @Override
    protected void configure() {

        install( new IndexModule() );

        //------------
        // COLLECTION
        //

//        // configure collections and our core astyanax framework
//        install(new CollectionModule());

        //------------
        // INDEX 
        //

//        install (new GuicyFigModule( IndexFig.class ));
//
//        install( new FactoryModuleBuilder()
//            .implement( EntityIndex.class, EsEntityIndexImpl.class )
//            .build( EntityIndexFactory.class ) );

        //------------
        // GRAPH 
        //

//        //install our configuration
//        install (new GuicyFigModule( GraphFig.class ));
//
//        bind( PostProcessObserver.class ).to( CollectionIndexObserver.class );
//
//        bind( EdgeMetadataSerialization.class).to( EdgeMetadataSerializationImpl.class);
//        bind( EdgeSerialization.class).to( EdgeSerializationImpl.class );
//        bind( NodeSerialization.class).to( NodeSerializationImpl.class );
//
//        bind( CassandraConfig.class).to( CassandraConfigImpl.class );
//
//        // create a guice factory for getting our collection manager
//        install( new FactoryModuleBuilder().implement( GraphManager.class, GraphManagerImpl.class )
//                                           .build( GraphManagerFactory.class ) );
//
//        //do multibindings for migrations
//        Multibinder<Migration> migrationBinding = Multibinder.newSetBinder( binder(), Migration.class );
//        migrationBinding.addBinding().to( EdgeMetadataSerializationImpl.class );
//        migrationBinding.addBinding().to( EdgeSerializationImpl.class );
//        migrationBinding.addBinding().to( NodeSerializationImpl.class );
//
//        // Graph event bus, will need to be refactored into it's own classes
//
//        // create a guice factor for getting our collection manager
//
//        // TODO: figure out why all this is necessary here but not in GraphModule
//        bind( new TypeLiteral<TimeoutQueue<Edge>>(){} )
//            .to( new TypeLiteral<LocalTimeoutQueue<Edge>>(){} );
//
//        bind( new TypeLiteral<TimeoutQueue<EdgeEvent<Edge>>>(){} )
//            .to( new TypeLiteral<LocalTimeoutQueue<EdgeEvent<Edge>>>(){} );
//        
//        bind( new TypeLiteral<TimeoutQueue<Id>>(){} )
//            .to( new TypeLiteral<LocalTimeoutQueue<Id>>(){} );
//
//        bind( new TypeLiteral<AsyncProcessor<Edge>>(){} )
//            .annotatedWith( EdgeDelete.class )
//            .to( new TypeLiteral<AsyncProcessorImpl<Edge>>(){} );
//
//        bind( new TypeLiteral<AsyncProcessor<EdgeEvent<Edge>>>(){} )
//            .annotatedWith( EdgeDelete.class )
//            .to( new TypeLiteral<AsyncProcessorImpl<EdgeEvent<Edge>>>(){} );
//
//        bind( new TypeLiteral<AsyncProcessor<Id>>(){} )
//            .annotatedWith( NodeDelete.class )
//            .to( new TypeLiteral<AsyncProcessorImpl<Id>>(){} );
//
//        //Repair/cleanup classes
//        bind( EdgeMetaRepair.class).to( EdgeMetaRepairImpl.class );
//        bind( EdgeDeleteRepair.class).to( EdgeDeleteRepairImpl.class );
//        bind( TimeService.class).to( TimeServiceImpl.class );
    }    
}
