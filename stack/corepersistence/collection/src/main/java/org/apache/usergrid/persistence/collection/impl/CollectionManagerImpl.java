package org.apache.usergrid.persistence.collection.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionManager;
import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.delete.DeleteStart;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.read.EventLoad;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.EventCreate;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.write.EventUpdate;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
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
    private final CollectionEventBus eventBus;


    @Inject
    public CollectionManagerImpl(  final CollectionEventBus eventBus,
                                  @Assisted final CollectionContext context ) {

        Preconditions.checkNotNull( eventBus, "eventBus must be defined" );
        Preconditions.checkNotNull( context, "context must be defined" );
        this.eventBus = eventBus;
        this.context = context;
    }


    @Override
    public Entity create( final Entity entity ) {
        // Create a new context for the write
        Result result = new Result();

        eventBus.post( new EventCreate( context, entity, result ) );

        MvccEntity completed = result.getLast( MvccEntity.class );

        return completed.getEntity().get();
    }


    @Override
    public Entity update( final Entity entity ) {
        // Create a new context for the write
        Result result = new Result();

        eventBus.post( new EventUpdate( context, entity, result ) );

        MvccEntity completed = result.getLast( MvccEntity.class );

        return completed.getEntity().get();
    }


    @Override
    public void delete( final UUID entityId ) {
        eventBus.post( new DeleteStart( context, entityId, null ) );
    }


    @Override
    public Entity load( final UUID entityId ) {
        Result result = new Result();

        eventBus.post( new EventLoad( context, entityId, result ) );

        MvccEntity completed = result.getLast( MvccEntity.class );

        return completed.getEntity().get();
    }
}
