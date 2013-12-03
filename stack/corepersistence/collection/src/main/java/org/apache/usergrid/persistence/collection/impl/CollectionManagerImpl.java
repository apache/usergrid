package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionManager;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.WriteContextFactory;
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
    private final TimeService timeService;
    private final WriteContextFactory factory;


    @Inject
    public CollectionManagerImpl( final TimeService timeService, final WriteContextFactory factory,
                                  @Assisted final CollectionContext context ) {
        this.context = context;
        this.timeService = timeService;
        this.factory = factory;
    }


    @Override
    public Entity create( final Entity entity ) {
        // Create a new context for the write
        WriteContext writeContext = factory.newCreateContext( context );

        //perform the write
        writeContext.performWrite( entity );

        //TODO this shouldn't block, give a callback
        return writeContext.getMessage( Entity.class );

    }


    @Override
    public Entity update( final Entity entity ) {
       return null;
    }


    @Override
    public void delete( final UUID entityId ) {
        WriteContext deleteContext = factory.newDeleteContext(context);

        deleteContext.performWrite( entityId );

        deleteContext.getMessage(Void.class);
    }


    @Override
    public Entity load( final UUID entityId ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
