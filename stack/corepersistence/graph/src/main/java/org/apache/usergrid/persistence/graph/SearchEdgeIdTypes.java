package org.apache.usergrid.persistence.graph;


import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Defines parameters for a search operation where searching from a node
 * using edge types.  Allows you to return all target types for that edge
 *
 * @author tnine */
public interface SearchEdgeIdTypes extends SearchEdgeTypes{


    /**
     * Return the edge type to use
     * @return
     */
    String getEdgeType();


}
