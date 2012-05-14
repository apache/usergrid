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

import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import me.prettyprint.hector.api.beans.HColumn;

import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.TypeParser;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;

/**
 * A simple class to make working with index buckets easier. Scans all buckets
 * and merges the results into a single column list to allow easy backwards
 * compatibility with existing code
 * 
 * @author tnine
 * 
 */
public class IndexBucketScanner {
    
    
    private final CassandraService cass;
    private final IndexBucketLocator indexBucketLocator;
    private final UUID applicationId;    
    private final Object keyPrefix;
    private final ApplicationCF columnFamily;
    private final Object start;
    private final Object finish;
    private final boolean reversed;
    private final int count;
    private final String[] indexPath;
    private final IndexType indexType;
    
    
    

    public IndexBucketScanner(CassandraService cass, IndexBucketLocator locator, ApplicationCF columnFamily, UUID applicationId, IndexType indexType, Object keyPrefix, Object start, Object finish, boolean reversed, int count, String... indexPath){
        this.cass = cass;
        this.indexBucketLocator = locator;
        this.applicationId = applicationId;
        this.keyPrefix = keyPrefix;
        this.columnFamily = columnFamily;
        this.start = start;
        this.finish = finish;
        this.reversed = reversed;
        this.count = count;
        this.indexPath = indexPath;
        this.indexType = indexType;
        
    }

    
    /**
     * Search the collection index using all the buckets for the given
     * collection
     * 
     * @param indexKey
     * @param slice
     * @param count
     * @param collectionName
     * @return
     * @throws Exception
     */
    public List<HColumn<ByteBuffer, ByteBuffer>> load()
            throws Exception {


        List<String> keys = indexBucketLocator.getBuckets(applicationId, indexType,
                indexPath);

        List<Object> cassKeys = new ArrayList<Object>(keys.size());

        for (String bucket : keys) {
            cassKeys.add(key(keyPrefix, bucket));
        }

        Map<ByteBuffer, List<HColumn<ByteBuffer, ByteBuffer>>> results = cass
                .multiGetColumns(cass.getApplicationKeyspace(applicationId),
                        columnFamily, cassKeys, start, finish, count,
                        reversed);

        // List<HColumn<ByteBuffer, ByteBuffer>> cols = new
        // ArrayList<HColumn<ByteBuffer, ByteBuffer>>();

        final Comparator<ByteBuffer> comparator = reversed ? new DynamicCompositeReverseComparator(columnFamily) : new DynamicCompositeForwardComparator(columnFamily);

        TreeSet<HColumn<ByteBuffer, ByteBuffer>> resultsTree = new TreeSet<HColumn<ByteBuffer, ByteBuffer>>(
                new Comparator<HColumn<ByteBuffer, ByteBuffer>>() {

                    @Override
                    public int compare(HColumn<ByteBuffer, ByteBuffer> first,
                            HColumn<ByteBuffer, ByteBuffer> second) {

                        return comparator.compare(first.getName(),
                                second.getName());
                    }

                });

        //TODO Todd Nine, this is O(n), and n will get quite large as our buckets expand.  We'll want to implement paging on row keys to limit in memory size 
        for (List<HColumn<ByteBuffer, ByteBuffer>> cols : results.values()) {

            for (HColumn<ByteBuffer, ByteBuffer> col : cols) {
                resultsTree.add(col);

                // trim if we're over size
                if (resultsTree.size() > count) {
                    resultsTree.remove(resultsTree.last());
                }
            }

        }

        return new ArrayList<HColumn<ByteBuffer, ByteBuffer>>(resultsTree);

    }
    
    private static abstract class DynamicCompositeComparator implements Comparator<ByteBuffer>{
        protected final AbstractType<ByteBuffer> dynamicComposite;
        
        @SuppressWarnings("unchecked")
        protected DynamicCompositeComparator(ApplicationCF cf){
            //should never happen, this will blow up during development if this fails
            try {
                dynamicComposite = TypeParser.parse(cf.getComparator());
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static class  DynamicCompositeForwardComparator extends DynamicCompositeComparator {

        /**
         * @param cf
         */
        protected DynamicCompositeForwardComparator(ApplicationCF cf) {
            super(cf);
        }

        @Override
        public int compare(ByteBuffer o1, ByteBuffer o2) {
            return dynamicComposite.compare(o1, o2);
        }
    }
    
    
    private static class DynamicCompositeReverseComparator  extends DynamicCompositeComparator {  
        /**
         * @param cf
         */
        protected DynamicCompositeReverseComparator(ApplicationCF cf) {
            super(cf);
        }

        @Override
        public int compare(ByteBuffer o1, ByteBuffer o2) {
            return dynamicComposite.compare(o2, o1);
        }
    }
}
