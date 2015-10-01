/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * LRU cache with per-entry timeout logic. (based on code from from Apache Roller)
 *
 * @author Dave Johnson
 */
public class LRUCache2<K, V> {

    private long timeout;
    private Map<K, CacheEntry> cache = null;
    private Environment environment = null;

    /**
     * Create cache.
     *
     * @param maxsize Maximum number of entries in cache.
     * @param timeout Entry timeout in milli-seconds.
     */
    public LRUCache2(int maxsize, long timeout) {
        this.environment = new DefaultEnvironment();
        this.timeout = timeout;
        this.cache = new LRULinkedHashMap(maxsize);
    }

    /**
     * Create cache that uses custom environment.
     *
     * @param maxsize Maximum number of entries in cache.
     * @param timeout Entry timeout in milli-seconds.
     */
    public LRUCache2(Environment environment, int maxsize, long timeout) {
        this.environment = environment;
        this.timeout = timeout;
        this.cache = new LRULinkedHashMap(maxsize);
    }

    public synchronized void put(K key, V value) {
        CacheEntry entry = new CacheEntry(value, environment.getCurrentTimeInMillis());
        cache.put(key, entry);
    }

    public V get(K key) {
        V value = null;
        CacheEntry<V> entry;
        synchronized (this) {
            entry = cache.get(key);
        }
        if (entry != null) {
            if (environment.getCurrentTimeInMillis() - entry.getTimeCached() < timeout) {
                value = entry.getValue();
            } else {
                cache.remove(entry);
            }
        }
        return value;
    }

    public synchronized void purge() {
        cache.clear();
    }

    public synchronized void purge(String[] patterns) {
        List<String> purgeList = new ArrayList<String>();
        for (Object objKey : cache.keySet()) {
            String key = (String) objKey;
            for (String s : patterns) {
                if (key.contains(s)) {
                    purgeList.add(key);
                    break;
                }
            }
        }
        for (String s : purgeList) {
            cache.remove(s);
        }
    }

    public int size() {
        return cache.size();
    }

    public interface Environment {

        long getCurrentTimeInMillis();
    }

    public static class DefaultEnvironment implements Environment {

        public long getCurrentTimeInMillis() {
            return System.currentTimeMillis();
        }
    }

    private static class CacheEntry<V> {

        private V value;
        private long timeCached = -1;

        public CacheEntry(V value, long timeCached) {
            this.timeCached = timeCached;
            this.value = value;
        }

        public long getTimeCached() {
            return timeCached;
        }

        public V getValue() {
            return value;
        }
    }

    // David Flanaghan: http://www.davidflanagan.com/blog/000014.html
    private static class LRULinkedHashMap<K,V> extends LinkedHashMap<K, CacheEntry> {

        protected int maxsize;

        public LRULinkedHashMap(int maxsize) {
            super(maxsize * 4 / 3 + 1, 0.75f, true);
            this.maxsize = maxsize;
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > this.maxsize;
        }
    }
}
