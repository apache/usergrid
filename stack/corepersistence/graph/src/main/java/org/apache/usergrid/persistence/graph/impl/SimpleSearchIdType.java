package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.SearchIdType;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 *
 *
 */
public class SimpleSearchIdType extends SimpleSearchEdgeType implements SearchIdType {

    private final String edgeType;


    public SimpleSearchIdType( final Id node, final String last, final String edgeType ) {
        super( node, last );

        ValidationUtils.verifyString( edgeType, "edgeType" );
        this.edgeType = edgeType;
    }


    @Override
    public String getEdgeType() {
       return edgeType;
    }
}
