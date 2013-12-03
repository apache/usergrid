package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.migration.Migration;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteStage;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccEntitySerializationStrategyImpl;
import org.apache.usergrid.persistence.collection.serialization.impl.MvccLogEntrySerializationStrategyImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;


/**
 * Simple module for wiring our pipelines
 *
 * @author tnine
 */
public class CollectionPipelineModule extends AbstractModule {


    /** Wire the pipeline of operations for create.  This should create a new
     * instance every time, since StagePipeline objects are mutable */
    @Provides
    @CreatePipeline
    @Inject
    public StagePipeline createWritePipeline(MvccEntityNew start, MvccEntityWrite write, MvccEntityCommit commit) {
        return StagePipelineImpl.fromStages(start, write, commit  );
    }


    @Provides
    @DeletePipeline
    public StagePipeline deletePipeline() {
        return StagePipelineImpl.fromStages(  );
    }



    @Override
    protected void configure() {

        /**
         * Configure all stages here
         */
        Multibinder<WriteStage> stageBinder = Multibinder.newSetBinder( binder(), WriteStage.class );

        stageBinder.addBinding().to( MvccEntityNew.class );
        stageBinder.addBinding().to( MvccEntityWrite.class );
        stageBinder.addBinding().to( MvccEntityCommit.class );


    }
}
