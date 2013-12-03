package org.apache.usergrid.persistence.collection.impl;


import org.apache.usergrid.persistence.collection.CollectionContext;
import org.apache.usergrid.persistence.collection.CollectionManager;
import org.apache.usergrid.persistence.collection.CollectionManagerFactory;


/**
 * Basic Imple
 * @author tnine
 */
public class CollectionManagerFactoryImpl implements CollectionManagerFactory {


    @Override
    public CollectionManager createCollectionManager( final CollectionContext context ) {
//        return new CollectionManagerImpl( context );
        return null;
    }
}
