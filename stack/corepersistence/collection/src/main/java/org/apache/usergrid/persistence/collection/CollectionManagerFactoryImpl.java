package org.apache.usergrid.persistence.collection;


/**
 * Basic Imple
 * @author tnine
 */
public class CollectionManagerFactoryImpl implements CollectionManagerFactory {


    @Override
    public CollectionManager createCollectionManager( final CollectionContext context ) {
        return new CollectionManagerImpl( context );
    }
}
