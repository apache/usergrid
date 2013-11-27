package org.apache.usergrid.persistence.collection;


/** A basic factory that creates a collection manager with the given context */
public interface CollectionManagerFactory
{

    /**
     * Create a new CollectionManager for the given context. The CollectionManager can safely be used on the current
     * thread and will cache responses.  The returned instance should not be shared among threads it will not be
     * guaranteed to be thread safe
     *
     * @param context The context to use when creating the collection manager
     *
     * @return The collection manager to perform operations within the provided context
     */
    public CollectionManager createCollectionManager( CollectionContext context );
}
