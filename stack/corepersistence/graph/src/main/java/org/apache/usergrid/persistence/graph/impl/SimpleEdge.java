package org.apache.usergrid.persistence.graph.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Simple bean to represent our edge
 * @author tnine
 */
public class SimpleEdge implements Edge {

    private final Id sourceNode;
    private final String type;
    private final Id targetNode;
    private final UUID version;


    public SimpleEdge( final Id sourceNode, final String type, final Id targetNode, final UUID version ) {

        ValidationUtils.verifyIdentity( sourceNode );
        ValidationUtils.verifyString( type, "type" );
        ValidationUtils.verifyIdentity( targetNode );
        ValidationUtils.verifyTimeUuid( version, "version" );
        this.sourceNode = sourceNode;
        this.type = type;
        this.targetNode = targetNode;
        this.version = version;
    }


    @Override
    public Id getSourceNode() {
        return sourceNode;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public Id getTargetNode() {
        return targetNode;
    }


    public UUID getVersion() {
        return version;
    }
}
