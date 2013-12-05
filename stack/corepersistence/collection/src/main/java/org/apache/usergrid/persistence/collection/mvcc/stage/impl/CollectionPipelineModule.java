package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionStage;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.Delete;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.DeletePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.StartDelete;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.read.Load;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.read.PipelineLoad;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Commit;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Create;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.PipelineCreate;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.PipelineUpdate;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.StartWrite;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Update;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.Verify;

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
    @PipelineCreate
    @Inject
    @Singleton
    public StagePipeline createPipeline( final Create create, final StartWrite startWrite, final Verify write,
                                         final Commit commit ) {
        return StagePipelineImpl.fromStages( create, startWrite, write, commit );
    }


    @Provides
    @PipelineUpdate
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
    public StagePipeline deletePipeline(final StartDelete startDelete,  final Delete delete ) {
        return StagePipelineImpl.fromStages(startDelete, delete );
    }


    @Provides
    @PipelineLoad
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
        Multibinder<ExecutionStage> stageBinder = Multibinder.newSetBinder( binder(), ExecutionStage.class );



        //creation stages
        stageBinder.addBinding().to( Commit.class );
        stageBinder.addBinding().to( Create.class );
        stageBinder.addBinding().to( StartWrite.class );
        stageBinder.addBinding().to( Update.class );
        stageBinder.addBinding().to( Verify.class );

        //delete stages
        stageBinder.addBinding().to( Delete.class );
        stageBinder.addBinding().to( StartDelete.class );

        //loading stages
        stageBinder.addBinding().to(Load.class);
    }
}
