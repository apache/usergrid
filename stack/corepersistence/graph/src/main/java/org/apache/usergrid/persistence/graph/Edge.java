package org.apache.usergrid.persistence.graph;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Defines a directed edge from the source node to the target node
 * @author tnine */
public interface Edge {

    /**
     * Get the Id of the source node of this edge
     * @return
     */
    Id getSourceNode();


    /**
     * Get the name of the edge
     * @return
     */
    String getType();

    /**
     * Get the id of the target node of this edge
     * @return
     */
    Id getTargetNode();

    /**
     * Get the version (as a type 1 time uuid) of this edge
     * @return
     */
    UUID getVersion();

}
