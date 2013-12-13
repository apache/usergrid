package org.apache.usergrid.persistence.graph;


import java.util.UUID;

import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Search by both edge type and target type.  Only nodes
 * with the type specified will be returned
 *
 * @author tnine */
public interface SearchByIdType extends SearchByEdgeType{

    /**
     * Get the target type in the search
     * @return
     */
    String getTargetType();


}
