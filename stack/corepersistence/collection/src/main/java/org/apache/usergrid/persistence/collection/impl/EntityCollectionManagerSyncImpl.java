package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerSync;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * A synchronous implementation that will block until the call is returned.
 */
public class EntityCollectionManagerSyncImpl implements EntityCollectionManagerSync {


    private final EntityCollectionManager em;


    @Inject
    public EntityCollectionManagerSyncImpl( final EntityCollectionManagerFactory emf,
                                            @Assisted final CollectionScope scope ) {

        //this feels a bit hacky, and I franky don't like it.  However, this is the only
        //way to get this to work I can find with guice, without having to manually implement the factory
        Preconditions.checkNotNull( emf, "entityCollectionManagerFactory is required" );
        Preconditions.checkNotNull( scope, "scope is required" );
        this.em = emf.createCollectionManager( scope );
    }


    @Override
    public Entity write( final Entity entity ) {
        return em.write( entity ).toBlockingObservable().single();
    }


    @Override
    public void delete( final Id entityId ) {
        em.delete( entityId ).toBlockingObservable().last();
    }


    @Override
    public Entity load( final Id entityId ) {
        return em.load( entityId ).toBlockingObservable().lastOrDefault( null );
    }
}
