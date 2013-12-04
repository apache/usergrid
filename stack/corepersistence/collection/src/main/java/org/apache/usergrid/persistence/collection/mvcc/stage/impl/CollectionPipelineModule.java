package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;


/**
 * Simple module for wiring our pipelines
 *
 * @author tnine
 */
public class CollectionPipelineModule extends AbstractModule {


    /**
     * Wire the pipeline of operations for create.  This should create a new instance every time, since StagePipeline
     * objects are mutable
     */
    @Provides
    @CreatePipeline
    @Inject
    @Singleton
    public StagePipeline createPipeline( final Create create, final Start start, final Verify write,
                                         final Commit commit ) {
        return StagePipelineImpl.fromStages( create, start, write, commit );
    }


    @Provides
    @UpdatePipeline
    @Inject
    @Singleton
    public StagePipeline updatePipeline( final Update update, final Start start, final Verify write,
                                         final Commit commit ) {
        return StagePipelineImpl.fromStages( update, start, write, commit );
    }


    @Provides
    @DeletePipeline
    @Inject
    @Singleton
    public StagePipeline deletePipeline( final Update update, final Start start, final Clear delete ) {
        return StagePipelineImpl.fromStages( update, start, delete );
    }


    @Override
    protected void configure() {

        /**
         * Configure all stages here
         */
        Multibinder<WriteStage> stageBinder = Multibinder.newSetBinder( binder(), WriteStage.class );


        stageBinder.addBinding().to( Create.class );
        stageBinder.addBinding().to( Update.class );
        stageBinder.addBinding().to( Start.class );
        stageBinder.addBinding().to( Verify.class );
        stageBinder.addBinding().to( Commit.class );
        stageBinder.addBinding().to( Clear.class );


    }
}
