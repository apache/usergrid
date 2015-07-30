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
package org.apache.usergrid.persistence;


import java.util.List;
import java.util.UUID;


/**
 * Interface for locating different buckets for indexing entities. These buckets are not intended for user with time
 * series indexing. Rather this a means of partitioning index puts across multiple rows
 *
 * @author tnine
 */
public interface IndexBucketLocator {


    /**
     * Return the bucket to use for indexing this entity
     * @param entityId The entity id to be indexed
     *
     * @return A bucket to use.  Note that ALL properties for the given entity should be in the same bucket.  This
     *         allows us to shard and execute queries in parallel.  Generally speaking, sharding on entityId is the best
     *         strategy, since this is an immutable value
     */
    String getBucket( UUID entityId );

    /**
     * Get all buckets that exist for this application with the given entity type, and property name

     *
     * @return All buckets for this application at the given component path
     */
    List<String> getBuckets();
}
