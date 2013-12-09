package org.apache.usergrid.persistence.graph;


import org.apache.usergrid.persistence.collection.Scope;


/**
 *
 * @author: tnine
 *
 */
public interface EdgeManagerFactory
{

    /**
     * Create an graph manager for the collection context
     *
     * @param scope The context to use when creating the graph manager
     */
    public EdgeManager createIndexManager( Scope scope );
}
