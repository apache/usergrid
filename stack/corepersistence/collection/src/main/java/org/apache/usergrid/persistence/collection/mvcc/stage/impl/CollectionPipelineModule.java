package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;


/**
 * Simple module for wiring our pipelines
 *
 * @author tnine
 */
public class CollectionPipelineModule extends AbstractModule {

    @Inject
    private MvccLogEntrySerializationStrategy mvccLogEntrySerializationStrategy;

    @Inject
    private MvccEntitySerializationStrategy mvccEntitySerializationStrategy;


    /** Wire the pipeline of operations for create.  This should create a new
     * instance every time, since StagePipeline objects are mutable */
    @Provides
    @CreatePipeline
    public StagePipeline createWritePipeline() {
        return StagePipelineImpl.fromStages( new Start( mvccLogEntrySerializationStrategy ), new Write(), new Commit() );
    }


    @Provides
    @DeletePipeline
    public StagePipeline deletePipeline() {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }


    @Provides
    @UpdatePipeline
    public StagePipeline updatePipeline() {
        return createWritePipeline();
    }


    @Override
    protected void configure() {
        //no op, we get our values from the provides above
    }
}
