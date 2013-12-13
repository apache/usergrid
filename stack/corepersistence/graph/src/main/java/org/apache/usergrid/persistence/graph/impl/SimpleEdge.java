package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * @author tnine
 */
public class SimpleEdge implements Edge {

    private final Id sourceNode;
    private final String type;
    private final Id targetNode;


    public SimpleEdge( final Id sourceNode, final String type, final Id targetNode ) {
        ValidationUtils.verifyIdentity(sourceNode);
        ValidationUtils.verifyString(type, "type");
        ValidationUtils.verifyIdentity( targetNode );
        this.sourceNode = sourceNode;
        this.type = type;
        this.targetNode = targetNode;
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
}
