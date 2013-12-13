package org.apache.usergrid.persistence.graph;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Defines parameters for a search operation where searching from a source node
 * using a specific type on the edge.  This will return edges with all target types
 *
 * @author tnine */
public interface SearchByEdgeType {

    /**
     * Get the Id of the node of this edge
     * @return
     */
    Id getNode();


    /**
     * Get the name of the edge
     * @return
     */
    String getType();

    /**
     * Get the Maximum Version of an edge we can return.
     * This should always be a type 1 time uuid.
     * @return
     */
    UUID getMaxVersion();

    /**
     * The optional start parameter.  All edges emitted with be > the specified start edge.
     * This is useful for paging.  Simply use the last value returned in the previous call in the start parameter
     * @return
     */
    Edge last();

}
