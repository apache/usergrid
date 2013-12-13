package org.apache.usergrid.persistence.graph;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Defines parameters for a search operation where searching from a node.
 * Useful for getting all types of edges relating to the node
 *
 *
 * @author tnine */
public interface SearchEdgeTypes {

    /**
     * Get the Id of the node of this edge
     * @return
     */
    Id getNode();


    /**
     * Return the last value returned.  All returned types will be >= this value
     * @return
     */
    String getLast();



}
