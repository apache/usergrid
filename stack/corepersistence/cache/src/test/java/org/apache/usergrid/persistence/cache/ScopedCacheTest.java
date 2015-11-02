/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.cache;


import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;


@RunWith( ITRunner.class )
@UseModules( { CacheTestModule.class } )
public class ScopedCacheTest {

    // this ensures that SCOPED_CACHE column family is created
    @Inject @Rule public MigrationManagerRule migrationManagerRule;

    @Inject protected CacheFactory<String, Map<String, Object>> cf;

    TypeReference typeRef = new TypeReference<HashMap<String, Object>>() {};


    @Test
    public void testBasicOperation() {

        CacheScope scope = new CacheScope( new SimpleId( "application" ) );
        ScopedCache<String, Map<String, Object>> cache = cf.getScopedCache(scope);
        assertNotNull("should get a cache", cache);

        Map<String, Object> item = new HashMap<String, Object>() {{
            put("field1", "value1");
        }};
        cache.put("item", item, 60);

        Map<String, Object> retrievedItem = cache.get("item", typeRef);
        assertNotNull( "should get back item", retrievedItem );
        assertEquals("value1", retrievedItem.get("field1"));
    }


    @Test
    public void testAppsGetSeparateCaches() {

        // create one app scoped cache and put a_value in it

        CacheScope scope1 = new CacheScope( new SimpleId( "application" ) );
        ScopedCache<String, Map<String, Object>> cache1 = cf.getScopedCache(scope1);

        Map<String, Object> item = new HashMap<String, Object>() {{
            put("field", "a_value");
        }};
        cache1.put("item", item, 60);

        // create a second app scoped cache and put another_value in it

        CacheScope scope2 = new CacheScope( new SimpleId( "application" ) );
        ScopedCache<String, Map<String, Object>> cache2 = cf.getScopedCache(scope2);

        item = new HashMap<String, Object>() {{
            put("field", "another_value");
        }};
        cache2.put("item", item, 60);

        Map<String, Object> fetched1 = cache1.get("item", typeRef);
        assertNotNull( "should get back item", fetched1 );
        assertEquals("a_value", fetched1.get("field"));

        Map<String, Object> fetched2 = cache2.get("item", typeRef);
        assertNotNull( "should get back item", fetched2 );
        assertEquals("another_value", fetched2.get("field"));
    }


    @Test
    public void testInvalidateAppCache() {

        CacheScope scope = new CacheScope( new SimpleId( "application" ) );
        ScopedCache<String, Map<String, Object>> cache = cf.getScopedCache(scope);
        assertNotNull("should get a cache", cache);

        // cache item forever

        Map<String, Object> item = new HashMap<String, Object>() {{
            put("field1", "value1");
        }};
        cache.put("item", item, 60);

        Map<String, Object> retrievedItem = cache.get("item", typeRef);
        assertNotNull( "should get back item", retrievedItem );
        assertEquals("value1", retrievedItem.get("field1"));

        cache.invalidate();

        assertNull(cache.get("item", typeRef));
    }


    @Test
    public void testTimeout() {

        CacheScope scope = new CacheScope( new SimpleId( "application" ) );
        ScopedCache<String, Map<String, Object>> cache = cf.getScopedCache(scope);
        assertNotNull("should get a cache", cache);

        // cache item for 1 second

        Map<String, Object> item = new HashMap<String, Object>() {{
            put("field1", "value1");
        }};
        cache.put("item", item, 1);

        Map<String, Object> retrievedItem = cache.get("item", typeRef);
        assertNotNull("should get back item", retrievedItem);
        assertEquals("value1", retrievedItem.get("field1"));

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        assertNull( cache.get("item", typeRef));
    }

    @Test
    public void testRemove() {

        CacheScope scope = new CacheScope( new SimpleId( "application" ) );
        ScopedCache<String, Map<String, Object>> cache = cf.getScopedCache(scope);
        assertNotNull("should get a cache", cache);

        // cache item for 1 second

        Map<String, Object> item = new HashMap<String, Object>() {{
            put("field1", "value1");
        }};
        cache.put("item", item, 1);

        Map<String, Object> retrievedItem = cache.get("item", typeRef);
        assertNotNull( "should get back item", retrievedItem );
        assertEquals("value1", retrievedItem.get("field1"));

        cache.remove("item");

        assertNull( cache.get("item", typeRef));
    }
}
