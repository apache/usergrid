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


import java.util.UUID;

import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;


/**
 * Class that performs validation on an entity to ensure it's in the shard we expect
 */
public final class ConnectionShardFilter implements ShardFilter {
    private final IndexBucketLocator indexBucketLocator;
    private final String expectedBucket;
    private final ConnectionRefImpl searchConnection;


    public ConnectionShardFilter( final IndexBucketLocator indexBucketLocator, final String expectedBucket,
                                  final ConnectionRefImpl connection ) {
        this.indexBucketLocator = indexBucketLocator;
        this.expectedBucket = expectedBucket;
        this.searchConnection = connection;


    }





    public boolean isInShard( final ScanColumn scanColumn ) {

        final UUID entityId = scanColumn.getUUID();

        final ConnectionRefImpl hashRef = new ConnectionRefImpl( searchConnection.getConnectingEntityType(), searchConnection.getConnectedEntityId(), searchConnection.getConnectionType(), searchConnection.getConnectingEntityType(), entityId  );

        final UUID hashId = hashRef.getConnectionSearchShardId();

        //not for our current processing shard, discard
        final String shard = indexBucketLocator.getBucket( hashId );

        return expectedBucket.equals( shard );
    }
}
