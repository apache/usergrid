package org.apache.usergrid.persistence.collection;


/**
 * A basic factory that creates a collection manager with the given context. Each instance of this factory should exist
 * for a Single OrganizationScope
 */
public interface EntityCollectionManagerFactory {

    /**
     * Create a new EntityCollectionManager for the given context. The EntityCollectionManager can safely be used on the
     * current thread and will cache responses.  The returned instance should not be shared among threads it will not be
     * guaranteed to be thread safe.
     *
     * @param collectionScope The collectionScope collectionScope to use when creating the collectionScope manager
     *
     * @return The collectionScope manager to perform operations within the provided context
     */
    public EntityCollectionManager createCollectionManager( CollectionScope collectionScope );



    /**
     * Create a new EntityCollectionManagerSync for the given context. The EntityCollectionManager can safely be used on the
     * current thread and will cache responses.  The returned instance should not be shared among threads it will not be
     * guaranteed to be thread safe.  This implementation will be synchronous.
     * Try to use the async implementation if possible
     *
     *
     * @param collectionScope The collectionScope collectionScope to use when creating the collectionScope manager
     *
     * @return The collectionScope manager to perform operations within the provided context
     */
    public EntityCollectionManagerSync createCollectionManagerSync( CollectionScope collectionScope );
}
