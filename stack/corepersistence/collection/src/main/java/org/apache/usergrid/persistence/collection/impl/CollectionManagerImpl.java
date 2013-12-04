package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionManager;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.CreatePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.DeletePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.ExecutionContextImpl;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.LoadPipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.UpdatePipeline;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * Simple implementation.  Should perform
 *
 * @author tnine
 */
public class CollectionManagerImpl implements CollectionManager {

    private static final Logger logger = LoggerFactory.getLogger( CollectionManagerImpl.class );

    private final CollectionContext context;
    private final StagePipeline createPipeline;
    private final StagePipeline updatePipeline;
    private final StagePipeline deletePipeline;
    private final StagePipeline loadPipeline;


    @Inject
    public CollectionManagerImpl( @CreatePipeline final StagePipeline createPipeline,
                                  @UpdatePipeline final StagePipeline updatePipeline,
                                  @DeletePipeline final StagePipeline deletePipeline,
                                  @LoadPipeline final StagePipeline loadPipeline,
                                  @Assisted final CollectionContext context ) {

        this.createPipeline = createPipeline;
        this.updatePipeline = updatePipeline;
        this.deletePipeline = deletePipeline;
        this.loadPipeline = loadPipeline;
        this.context = context;
    }


    @Override
    public Entity create( final Entity entity ) {
        // Create a new context for the write
        ExecutionContext executionContext = new ExecutionContextImpl( createPipeline, context );

        //perform the write
        executionContext.execute( entity );

        MvccEntity result = executionContext.getMessage( MvccEntity.class );

        return result.getEntity().get();
    }


    @Override
    public Entity update( final Entity entity ) {
        // Create a new context for the write
        ExecutionContext executionContext = new ExecutionContextImpl( updatePipeline, context );

        //perform the write
        executionContext.execute( entity );

        MvccEntity result = executionContext.getMessage( MvccEntity.class );

         return result.getEntity().get();
    }


    @Override
    public void delete( final UUID entityId ) {
        ExecutionContext deleteContext = new ExecutionContextImpl( deletePipeline, context );

        deleteContext.execute( entityId );
    }


    @Override
    public Entity load( final UUID entityId ) {
        ExecutionContext loadContext = new ExecutionContextImpl( loadPipeline, context );

        loadContext.execute( entityId );

        return loadContext.getMessage( Entity.class );

    }
}
