package org.apache.usergrid.persistence.collection.mvcc.stage.impl;


import java.util.Collection;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.mvcc.event.PostProcessListener;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContextFactory;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/** @author tnine */
@Singleton
public class WriteContextFactoryImpl implements WriteContextFactory {

    private final StagePipeline writeStage;
    private final StagePipeline deleteStage;
    private final Collection<PostProcessListener> postProcessListener;


    @Inject
    public WriteContextFactoryImpl( @CreatePipeline final StagePipeline writeStage,
                                    @DeletePipeline final StagePipeline deleteStage,
                                    final Collection<PostProcessListener> postProcessListener ) {
        this.writeStage = writeStage;
        this.deleteStage = deleteStage;
        this.postProcessListener = postProcessListener;
    }


    @Override
    public WriteContext newCreateContext(CollectionContext context) {
        return new WriteContextImpl( postProcessListener, writeStage, context );
    }


    @Override
    public WriteContext newDeleteContext(CollectionContext context) {
        return new WriteContextImpl( postProcessListener, deleteStage, context );
    }
}
