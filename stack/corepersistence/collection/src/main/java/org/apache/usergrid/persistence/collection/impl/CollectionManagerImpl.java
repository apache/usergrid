package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionManager;
import org.apache.usergrid.persistence.collection.mvcc.stage.StagePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.CreatePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.DeletePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.UpdatePipeline;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.WriteContextImpl;
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


    @Inject
    public CollectionManagerImpl( @CreatePipeline final StagePipeline createPipeline,
                                  @UpdatePipeline final StagePipeline updatePipeline,
                                  @DeletePipeline final StagePipeline deletePipeline,
                                  @Assisted final CollectionContext context) {
        this.context = context;
        this.createPipeline = createPipeline;
        this.updatePipeline = updatePipeline;
        this.deletePipeline = deletePipeline;
    }


    @Override
    public Entity create( final Entity entity ) {
        // Create a new context for the write
        WriteContext writeContext = new WriteContextImpl( createPipeline, context );

        //perform the write
        writeContext.performWrite( entity );

        return writeContext.getMessage( Entity.class );
    }


    @Override
    public Entity update( final Entity entity ) {
        // Create a new context for the write
        WriteContext writeContext = new WriteContextImpl( updatePipeline, context );

        //perform the write
        writeContext.performWrite( entity );

        return writeContext.getMessage( Entity.class );
    }


    @Override
    public void delete( final UUID entityId ) {
        WriteContext deleteContext =  new WriteContextImpl( deletePipeline, context );

        deleteContext.performWrite( entityId );

    }


    @Override
    public Entity load( final UUID entityId ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
