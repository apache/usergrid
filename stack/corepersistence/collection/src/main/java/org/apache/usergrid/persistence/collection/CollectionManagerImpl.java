package org.apache.usergrid.persistence.collection;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Entity;


/**
 * Simple implementation.  Should perform
 * @author tnine
 */
public class CollectionManagerImpl implements CollectionManager {

    private final CollectionContext context;


    public CollectionManagerImpl( final CollectionContext context ) {
        this.context = context;
    }


    @Override
    public void create( final Entity entity ) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public void update( final Entity entity ) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public void delete( final UUID entityId ) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public Entity load( final UUID entityId ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
