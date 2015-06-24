/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.index.IndexScanner;
import org.apache.usergrid.persistence.query.ir.QuerySlice;

import me.prettyprint.hector.api.beans.HColumn;


/**
 * An iterator that will check if the parsed column is part of this shard.  This is required due to a legacy storage
 * format
 *
 * Connections are not sharded by target entity, as a result, we get data partition mismatches when performing
 * intersections and seeks.  This is meant to discard target entities that are not part of the current shard
 *
 * @author tnine
 */
public class SliceShardIterator extends SliceIterator {

    private static final Logger logger = LoggerFactory.getLogger( SliceShardIterator.class );

    private final ShardBucketValidator shardBucketValidator;


    /**
     * @param slice The slice used in the scanner
     * @param scanner The scanner to use to read the cols
     * @param parser The parser for the scanner results
     */
    public SliceShardIterator( final ShardBucketValidator shardBucketValidator, final QuerySlice slice,
                               final IndexScanner scanner, final SliceParser parser ) {
        super( slice, scanner, parser );

        this.shardBucketValidator = shardBucketValidator;
    }


    /**
     * Parses the column.  If the column should be discarded, null should be returned
     */
    protected ScanColumn parse( HColumn<ByteBuffer, ByteBuffer> column ) {

        final ByteBuffer colName = column.getName().duplicate();

        final ScanColumn parsed = parser.parse( colName, isReversed );

        if(parsed == null){
            return null;
        }

        final UUID entityId = parsed.getUUID();


        //not for our current processing shard, discard
        if(!shardBucketValidator.isInShard( entityId )){
            return null;
        }

        return parsed;
    }


    /**
     * Class that performs validation on an entity to ensure it's in the shard we expecte
     */
    public static final class ShardBucketValidator {
        private final IndexBucketLocator indexBucketLocator;
        private final String expectedBucket;
        private final UUID applicationId;
        private final IndexBucketLocator.IndexType type;
        private final String[] components;


        public ShardBucketValidator( final IndexBucketLocator indexBucketLocator, final String expectedBucket,
                                     final UUID applicationId, final IndexBucketLocator.IndexType type,
                                     final String... components ) {
            this.indexBucketLocator = indexBucketLocator;
            this.expectedBucket = expectedBucket;
            this.applicationId = applicationId;
            this.type = type;
            this.components = components;
        }


        public boolean isInShard( final UUID entityId ) {
            //not for our current processing shard, discard
            final String shard = indexBucketLocator.getBucket( applicationId, type, entityId, components );

            return expectedBucket.equals( shard );
        }
    }
}
