package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.SearchEdgeIdType;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 */
public class SimpleSearchEdgeIdType extends SimpleSearchEdgeType implements SearchEdgeIdType {

    private final String edgeType;


    public SimpleSearchEdgeIdType( final Id node, final String last, final String edgeType ) {
        super( node, last );

        ValidationUtils.verifyString( edgeType, "edgeType" );
        this.edgeType = edgeType;
    }


    @Override
    public String getEdgeType() {
       return edgeType;
    }
}
