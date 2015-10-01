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

import org.apache.usergrid.persistence.graph.SearchByEdgeType;


/**
 * Comparator that will compare in reverse or forward order based on the order type specified.
 *
 * Assumes descending uses the default order.  If ASCENDING, the result of the comparator will be reversed
 */
public class OrderedComparator<T> implements Comparator<T> {


    private final int invert;
    private final Comparator<T> delegate;


    public OrderedComparator( final Comparator<T> delegate, final SearchByEdgeType.Order order ) {
        this.invert = order == SearchByEdgeType.Order.DESCENDING ? 1 : -1;
        this.delegate = delegate;
    }


    @Override
    public int compare( final T o1, final T o2 ) {
        return delegate.compare( o1, o2 ) * invert;
    }
}
