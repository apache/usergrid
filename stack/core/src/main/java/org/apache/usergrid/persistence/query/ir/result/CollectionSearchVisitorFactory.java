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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.cassandra.QueryProcessor;
import org.apache.usergrid.persistence.query.ir.SearchVisitor;


/**
 * Creates collection visitors per shard
 */
public class CollectionSearchVisitorFactory implements SearchVisitorFactory {

    private final CassandraService cassandraService;
    private final IndexBucketLocator indexBucketLocator;
    private final QueryProcessor queryProcessor;
    private final UUID applicationId;
    private final EntityRef headEntity;


    public CollectionSearchVisitorFactory( final CassandraService cassandraService,
                                           final IndexBucketLocator indexBucketLocator,
                                           final QueryProcessor queryProcessor, final UUID applicationId,
                                           final EntityRef headEntity, final String collectionName ) {
        this.cassandraService = cassandraService;
        this.indexBucketLocator = indexBucketLocator;
        this.queryProcessor = queryProcessor;
        this.applicationId = applicationId;
        this.headEntity = headEntity;
    }


    @Override
    public Collection<SearchVisitor> createVisitors() {

        final List<String> buckets =
                indexBucketLocator.getBuckets( );


        final List<SearchVisitor> visitors = new ArrayList<SearchVisitor>( buckets.size() );

        for ( final String bucket : buckets ) {

            final SearchVisitor searchVisitor =
                    new SearchCollectionVisitor( cassandraService, indexBucketLocator, queryProcessor, applicationId,
                            headEntity, bucket );
            visitors.add( searchVisitor );
        }


        return visitors;
    }
}
