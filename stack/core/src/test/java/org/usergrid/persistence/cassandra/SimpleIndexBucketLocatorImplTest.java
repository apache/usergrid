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
package org.usergrid.persistence.cassandra;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.utils.UUIDUtils;

/**
 * @author tnine
 * 
 */
public class SimpleIndexBucketLocatorImplTest {

    @Test
    public void loadBuckets() {

        UUID appId = UUIDUtils.newTimeUUID();
        String entityType = "user";
        String propName = "firstName";

        SimpleIndexBucketLocatorImpl locator = new SimpleIndexBucketLocatorImpl();
        
        List<String> buckets = locator.getBuckets(appId, entityType, propName);
        
        
        
        UUID testId1 = UUIDUtils.newTimeUUID(0l);
        
        UUID testId2 =  UUIDUtils.newTimeUUID(Long.MAX_VALUE/2);
        
        UUID testId3 = UUIDUtils.newTimeUUID(Long.MAX_VALUE);
        
        
        String bucket1 = locator.getBucket(appId, entityType, testId1, propName);
        
        String bucket2 = locator.getBucket(appId, entityType, testId2, propName);
        
        String bucket3 = locator.getBucket(appId, entityType, testId3, propName);
        
        

    }

}
