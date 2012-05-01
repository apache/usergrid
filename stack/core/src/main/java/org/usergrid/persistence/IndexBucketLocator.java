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
 * Interface for locating different buckets for indexing entities.  These buckets are not intended
 * for user with time series indexing.  Rather this a means of partitioning index puts across multiple rows  
 * 
 * @author tnine
 *
 */
public interface IndexBucketLocator {

    /**
     * Return the bucket to use for indexing this entity 
     * 
     * @param applicationId
     * @param entityType
     * @param entityId
     * @param propertyName
     * @return
     */
    public String getBucket(UUID applicationId, String entityType, UUID entityId, String propertyName);
    
    
    /**
     * Get all buckets that exist for this application with the given entity type, and property name
     * @param applicationId
     * @param entityType
     * @param propertyName
     * @return
     */
    public List<String> getBuckets(UUID applicationId, String entityType, String propertyName);
}
