package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.CollectionEventBusImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.EventStage;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.Delete;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.StartDelete;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.load.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Commit;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.StartWrite;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Verify;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;


/**
 * Simple module for wiring our pipelines
 *
 * @author tnine
 */
public class CollectionPipelineModule extends AbstractModule {


    @Provides
    @Singleton
    public CollectionEventBus eventBus(){
        CollectionEventBus bus =  new CollectionEventBusImpl( "collection" );

        return bus;
    }


    @Override
    protected void configure() {

        /**
         * Configure all stages here
         */
        Multibinder<EventStage> stageBinder = Multibinder.newSetBinder( binder(), EventStage.class );


        /**
         * Note we have to have the .asEagerSingleton(); or guice never loads these impls b/c they aren't
         * directly referenced
         */

        //creation stages

        stageBinder.addBinding().to( StartWrite.class ).asEagerSingleton();;
        stageBinder.addBinding().to( Verify.class ).asEagerSingleton();;
        stageBinder.addBinding().to( Commit.class ).asEagerSingleton();

        //delete stages
        stageBinder.addBinding().to( Delete.class ).asEagerSingleton();;
        stageBinder.addBinding().to( StartDelete.class ).asEagerSingleton();;

        //loading stages
        stageBinder.addBinding().to(Load.class).asEagerSingleton();;

    }
}
