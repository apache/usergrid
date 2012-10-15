/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence;

import java.util.List;
import java.util.UUID;

/**
 * Interface for locating different buckets for indexing entities. These buckets
 * are not intended for user with time series indexing. Rather this a means of
 * partitioning index puts across multiple rows
 * 
 * @author tnine
 * 
 */
public interface IndexBucketLocator {

    public enum IndexType {
        COLLECTION("collection"), CONNECTION("connection"), GEO("geo"), UNIQUE("unique");

        private final String type;

        private IndexType(String type) {
            this.type = type;
        }
        
        public String getType(){
            return type;
        }

    }

    /**
     * Return the bucket to use for indexing this entity
     * 
     * @param applicationId
     *            The application id
     * 
     * @param type
     *            The type of the index. This way indexing on the same property
     *            value for different types of indexes does not cause collisions
     *            on partitioning and lookups
     * 
     * @param entityId
     *            The entity id to be indexed
     * 
     * @param components
     *            The strings and uniquely identify the path to this index. I.E
     *            entityType and propName, collection name etc This string must
     *            remain the same for all reads and writes
     * @return
     */
    public String getBucket(UUID applicationId, IndexType type, UUID entityId,
            String... components);

    /**
     * Get all buckets that exist for this application with the given entity
     * type, and property name
     * 
     * @param applicationId
     *            The application id
     * 
     * @param type
     *            The type of index
     * @param components
     *            The strings and uniquely identify the path to this index. I.E
     *            entityType and propName, collection name etc
     * @return All buckets for this application at the given component path
     */
    public List<String> getBuckets(UUID applicationId, IndexType type,
            String... components);
}
