package org.apache.usergrid.persistence.graph.impl;


import org.apache.usergrid.persistence.collection.mvcc.entity.ValidationUtils;
import org.apache.usergrid.persistence.graph.SearchEdgeIdTypes;
import org.apache.usergrid.persistence.graph.SearchEdgeTypes;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 *
 *
 */
public class SimpleSearchEdgeIdTypes extends SimpleSearchEdgeTypes implements SearchEdgeIdTypes {

    private final String edgeType;


    public SimpleSearchEdgeIdTypes( final Id node, final String last, final String edgeType ) {
        super( node, last );

        ValidationUtils.verifyString( edgeType, "edgeType" );
        this.edgeType = edgeType;
    }


    @Override
    public String getEdgeType() {
       return edgeType;
    }
}
