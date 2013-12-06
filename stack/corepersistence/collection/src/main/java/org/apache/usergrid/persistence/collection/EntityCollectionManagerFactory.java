package org.apache.usergrid.persistence.collection;


/** A basic factory that creates a collection manager with the given context */
public interface EntityCollectionManagerFactory {

    /**
     * Create a new EntityCollectionManager for the given context. The EntityCollectionManager can safely be used on the
     * current thread and will cache responses.  The returned instance should not be shared among threads it will not be
     * guaranteed to be thread safe
     *
     * @param entityCollection The entityCollection scope to use when creating the entityCollection manager
     *
     * @return The entityCollection manager to perform operations within the provided context
     */
    public EntityCollectionManager createCollectionManager( EntityCollection entityCollection );
}
