/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.query.ir;


import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.usergrid.persistence.query.ir.QuerySlice.RangeValue;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.indexValueCode;
import static org.apache.usergrid.persistence.cassandra.IndexUpdate.toIndexableValue;


/**
 * A node which has 1 or more query Slices that can be unioned together. I.E and && operation with either 1 or more
 * children
 *
 * @author tnine
 */
public class SliceNode extends QueryNode {

    /**
     * A context within a tree to allow for operand and range scan optimizations. In the event that the user enters a
     * query in the following way
     * <p/>
     * (x > 5 and x < 15 and z > 10 and z < 20) or (y > 10 and y < 20)
     * <p/>
     * You will have 2 contexts. The first is for (x > 5 and x < 15 and z > 10 and z < 20), the second is for (y > 10
     * and y < 20). This allows us to compress these operations into a single range scan per context.
     */
    // private class TreeContext {

    private Map<String, QuerySlice> pairs = new LinkedHashMap<String, QuerySlice>();

    private int id;


    /**
     * Set the id for construction. Just a counter. Used for creating tokens and things like tokens where the same
     * property can be used in 2 different subtrees
     */
    public SliceNode( int id ) {
        this.id = id;
    }


    /**
     * Set the start value. If the range pair doesn't exist, it's created
     *
     * @param start The start value. this will be processed and turned into an indexed value
     */
    public void setStart( String fieldName, Object start, boolean inclusive ) {
        QuerySlice slice = getOrCreateSlice( fieldName );

        // if the value is null don't set the range on the slice
        if ( start == null ) {
            return;
        }

        RangeValue existingStart = slice.getStart();

        Object indexedValue = toIndexableValue( start );
        byte code = indexValueCode( indexedValue );

        RangeValue newStart = new RangeValue( code, indexedValue, inclusive );

        if ( existingStart == null ) {
            slice.setStart( newStart );
            return;
        }

        // check if we're before the currently set start in this
        // context. If so set the value to increase the range scan size;
        if ( existingStart != null && newStart == null || ( existingStart != null
                && existingStart.compareTo( newStart, false ) < 0 ) ) {
            slice.setStart( newStart );
        }
    }


    /** Set the finish. If finish value is greater than the existing, I.E. null or higher comparison, then */
    public void setFinish( String fieldName, Object finish, boolean inclusive ) {
        QuerySlice slice = getOrCreateSlice( fieldName );

        // if the value is null don't set the range on the slice
        if ( finish == null ) {
            return;
        }

        RangeValue existingFinish = slice.getFinish();

        Object indexedValue = toIndexableValue( finish );
        byte code = indexValueCode( indexedValue );

        RangeValue newFinish = new RangeValue( code, indexedValue, inclusive );

        if ( existingFinish == null ) {
            slice.setFinish( newFinish );
            return;
        }

        // check if we're before the currently set start in this
        // context. If so set the value to increase the range scan size;
        if ( existingFinish != null && newFinish == null || ( existingFinish != null
                && existingFinish.compareTo( newFinish, false ) < 0 ) ) {
            slice.setFinish( newFinish );
        }
    }


    /** Lazy instanciate a field pair if required. Otherwise return the existing pair */
    private QuerySlice getOrCreateSlice( String fieldName ) {
        QuerySlice pair = this.pairs.get( fieldName );

        if ( pair == null ) {
            pair = new QuerySlice( fieldName, id );
            this.pairs.put( fieldName, pair );
        }

        return pair;
    }


    /** Get the slice by field name if it exists. Null otherwise */
    public QuerySlice getSlice( String fieldName ) {
        return this.pairs.get( fieldName );
    }


    /** Get all slices in our context */
    public Collection<QuerySlice> getAllSlices() {
        return this.pairs.values();
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.ir.QueryNode#visit(org.apache.usergrid.persistence
     * .query.ir.NodeVisitor)
     */
    @Override
    public void visit( NodeVisitor visitor ) throws Exception {
        visitor.visit( this );
    }


    @Override
    public String toString() {
        return "SliceNode [pairs=" + pairs + ", id=" + id + "]";
    }
}
