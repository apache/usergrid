/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.graph.serialization.impl.shard.impl.comparators;


import java.util.Comparator;
import java.util.UUID;

import org.apache.usergrid.persistence.graph.MarkedEdge;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.fasterxml.uuid.impl.UUIDUtil;


/**
 * Comparator for comparing edges in descending order.  The first comparison is the timestamp,
 * highest value should be first, so is considered "less".  If those are equal, the UUIId is compared.
 * this will return the UUID to compare.  It will first be descending UUID, then ascending name
 *
 */
public abstract class DirectedEdgeDescendingComparator implements Comparator<MarkedEdge> {

    @Override
    public int compare( final MarkedEdge first, final MarkedEdge second ) {

        int compare = Long.compare( first.getTimestamp(), second.getTimestamp() ) * -1;

        if(compare == 0){
            final Id firstId = getId( first );
            final Id secondId = getId( second );

            compare = UUIDComparator.staticCompare( firstId.getUuid(), secondId.getUuid() ) * -1;

            if(compare == 0){
                compare = firstId.getType().compareTo( secondId.getType() );
            }
        }

        return compare;
    }


    /**
     * Return the Id to be used in the comparison
     * @param edge
     * @return
     */
    protected abstract Id getId(final MarkedEdge edge);
}
