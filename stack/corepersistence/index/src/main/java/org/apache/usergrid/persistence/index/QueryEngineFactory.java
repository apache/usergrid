package org.apache.usergrid.persistence.index;


import org.apache.usergrid.persistence.collection.EntityCollection;


/**
 *
 * @author: tnine
 *
 */
public interface QueryEngineFactory
{

    /**
     * Create an index manager for the collection context
     *
     * @param context The context to use when creating the index manager
     */
    public QueryEngineFactory createIndexManager( EntityCollection context );
}
