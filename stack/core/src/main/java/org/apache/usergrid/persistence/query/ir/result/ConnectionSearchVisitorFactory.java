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
package org.apache.usergrid.persistence.query.ir.result;


import java.util.Collection;

import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.QueryProcessor;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;


public class ConnectionSearchVisitorFactory implements SearchVisitorFactory {

    private final
    private final IndexBucketLocator indexBucketLocator;
    private final QueryProcessor queryProcessor;
    private final ConnectionRef connectionRef;
    private final boolean outgoing;

    private ConnectionSearchVisitorFactory( final IndexBucketLocator indexBucketLocator,
                                            final QueryProcessor queryProcessor, final ConnectionRef connectionRef,
                                            final boolean outgoing ){
//        SearchConnectionVisitor visitor = new SearchConnectionVisitor( indexBucketLocator, qp, connectionRef, true  );

        this.indexBucketLocator = indexBucketLocator;
        this.queryProcessor = queryProcessor;
        this.connectionRef = connectionRef;
        this.outgoing = outgoing;
    }


    @Override
    public Collection<SearchVisitor> createVisitors() {

        indexBucketLocator.getBuckets(  )


        return null;
    }
}
