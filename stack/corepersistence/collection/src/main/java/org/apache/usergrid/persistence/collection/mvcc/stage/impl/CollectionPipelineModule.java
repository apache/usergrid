package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.stage.Stage;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;

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
    public StagePipeline createPipeline( final Create create, final StartWrite startWrite, final Verify write,
                                         final Commit commit ) {
        return StagePipelineImpl.fromStages( create, startWrite, write, commit );
    }


    @Provides
    @UpdatePipeline
    @Inject
    @Singleton
    public StagePipeline updatePipeline( final Update update, final StartWrite startWrite, final Verify write,
                                         final Commit commit ) {
        return StagePipelineImpl.fromStages( update, startWrite, write, commit );
    }


    @Provides
    @DeletePipeline
    @Inject
    @Singleton
    public StagePipeline deletePipeline(final StartDelete startDelete,  final Clear delete ) {
        return StagePipelineImpl.fromStages(startDelete, delete );
    }


    @Provides
    @LoadPipeline
    @Inject
    @Singleton
    public StagePipeline deletePipeline( final Load load ) {
        return StagePipelineImpl.fromStages( load );
    }


    @Override
    protected void configure() {

        /**
         * Configure all stages here
         */
        Multibinder<Stage> stageBinder = Multibinder.newSetBinder( binder(), Stage.class );



        //creation stages
        stageBinder.addBinding().to( Create.class );
        stageBinder.addBinding().to( Update.class );
        stageBinder.addBinding().to( StartWrite.class );
        stageBinder.addBinding().to( Verify.class );
        stageBinder.addBinding().to( Commit.class );
        stageBinder.addBinding().to( Clear.class );

        //loading stages
        stageBinder.addBinding().to(Load.class);
    }
}
