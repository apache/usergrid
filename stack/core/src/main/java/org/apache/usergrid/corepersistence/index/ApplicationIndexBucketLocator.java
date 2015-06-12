/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.corepersistence.index;

import com.google.common.hash.Funnel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.shard.ExpandingShardLocator;
import org.apache.usergrid.persistence.core.shard.StringHashUtils;

/**
 * Strategy for getting index buckets, needs to be held in memory
 */
@Singleton
public class ApplicationIndexBucketLocator{

    /**
     * Number of buckets to hash across.
     */
    private final int numberOfBuckets ;
    /**
     * How to funnel keys for buckets
     */
    private final Funnel<String> mapKeyFunnel;
    /**
     * Locator to get us all buckets
     */
    private  final ExpandingShardLocator<String> bucketLocator ;
    /**
     * Startseed for buckets
     */
    private final int indexBucketOffset;


    @Inject
    public ApplicationIndexBucketLocator(CoreIndexFig indexFig){
        numberOfBuckets = indexFig.getNumberOfIndexBuckets();
        mapKeyFunnel = (key, into) -> into.putString( key, StringHashUtils.UTF8 );
        indexBucketOffset = indexFig.getBucketOffset();
        bucketLocator = new ExpandingShardLocator<>(mapKeyFunnel, numberOfBuckets);
    }

    public int getBucket(ApplicationScope applicationScope){
        //potentially add offset to remove old buckets
        //if set is 1-5 then +5 would change range to 6-10
        return indexBucketOffset + bucketLocator.getCurrentBucket(applicationScope.getApplication().getUuid().toString());
    }
}
