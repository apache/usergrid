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
package org.usergrid.tools;

import static me.prettyprint.hector.api.factory.HFactory.createMutator;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.persistence.cassandra.IndexUpdate.indexValueCode;
import static org.usergrid.utils.ConversionUtils.bytes;

import java.nio.ByteBuffer;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.mutation.Mutator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.DynamicEntity;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.cassandra.EntityManagerImpl;
import org.usergrid.utils.UUIDUtils;

/**
 * 
 * A utility to insert entities into the em for benchmarking
 * 
 * @author tnine
 * 
 */
public class EntityInsertBenchMark extends ToolBase {

   public static final ByteBufferSerializer be = new ByteBufferSerializer();

    private static final Logger logger = LoggerFactory.getLogger(EntityInsertBenchMark.class);

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Option hostOption = OptionBuilder.withArgName("host").hasArg().isRequired(true)
                .withDescription("Cassandra host").create("host");

        Option countOption = OptionBuilder.withArgName("count").hasArg().isRequired(true)
                .withDescription("Number of records").create("count");

        Option appIdOption = OptionBuilder.withArgName("appId").hasArg().isRequired(true)
                .withDescription("Application Id to use").create("appId");

        Option workerOption = OptionBuilder.withArgName("workers").hasArg().isRequired(true)
                .withDescription("Number of workers to use").create("workers");
       
    
        
        Options options = new Options();
        options.addOption(hostOption);
        options.addOption(countOption);
        options.addOption(appIdOption);
        options.addOption(workerOption);

        return options;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.usergrid.tools.ToolBase#runTool(org.apache.commons.cli.CommandLine)
     */
    @Override
    public void runTool(CommandLine line) throws Exception {
        startSpring();

        logger.info("Starting entity cleanup");
        
        int workerSize = Integer.parseInt(line.getOptionValue("workers"));


        ExecutorService executors = Executors.newFixedThreadPool(workerSize);

        int count = Integer.parseInt(line.getOptionValue("count"));

        int size = count / workerSize;

        UUID appId = UUID.fromString(line.getOptionValue("appId"));

        Stack<Future<Void>> futures = new Stack<Future<Void>>();

        for (int i = 0; i < workerSize; i++) {
            futures.push(executors.submit(new InsertWorker(i, size, appId)));
        }

        System.out.println("Waiting for workers to complete insertion");

        /**
         * Wait for all tasks to complete
         */
        while (!futures.isEmpty()) {
            futures.pop().get();
        }

        System.out.println("All workers completed insertion");

    }

    private class InsertWorker implements Callable<Void> {

        private int count;

        private int workerNumber;

        private UUID appId;

        private InsertWorker(int workerNumber, int count, UUID appId) {
            this.workerNumber = workerNumber;
            this.count = count;
            this.appId = appId;

        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public Void call() throws Exception {

            Keyspace ko = EntityInsertBenchMark.this.cass.getApplicationKeyspace(appId);
            EntityManagerImpl em = (EntityManagerImpl) emf.getEntityManager(appId);
            IndexBucketLocator indexBucketLocator = em.getIndexBucketLocator();

            for (int i = 0; i < count; i++) {

                Mutator<ByteBuffer> m = createMutator(ko, be);

                DynamicEntity dynEntity = new DynamicEntity();
                dynEntity.setType("test");
                dynEntity.setUuid(UUIDUtils.newTimeUUID());

                String value = new StringBuilder().append(workerNumber).append("-").append(i).toString();

             
                String bucketId = indexBucketLocator
                        .getBucket(appId, IndexType.COLLECTION, dynEntity.getUuid(), "test");

                Object index_name = key(appId, "tests", "test", bucketId);

                IndexEntry entry = new IndexEntry(dynEntity.getUuid(), "test", value, UUIDUtils.newTimeUUID());

                addInsertToMutator(m, ENTITY_INDEX, index_name, entry.getIndexComposite(), null,
                        System.currentTimeMillis());

                UniqueIndexer indexer = new UniqueIndexer(m);
                indexer.writeIndex(appId, "tests", dynEntity.getUuid(), "test", value);
                // write this to the direct collection index

                m.execute();

                if (i % 100 == 0) {
                    System.out.println(String.format("%s : Written %d of %d", Thread.currentThread().getName(), i,
                            count));
                }
            }

            return null;
        }

    }

    private class UniqueIndexer {

         private Mutator<ByteBuffer> mutator;

        /**
         * @param indexBucketLocator
         * @param mutator
         */
        public UniqueIndexer(Mutator<ByteBuffer> mutator) {
            super();
            this.mutator = mutator;
        }

        private void writeIndex(UUID applicationId, String collectionName, UUID entityId, String propName,
                Object entityValue) {

            Object rowKey = key(applicationId, collectionName, propName, md5(bytes(entityValue)));

            addInsertToMutator(mutator, ENTITY_UNIQUE, rowKey, entityId, null,
                    System.currentTimeMillis());

        }

    }
    

    public static class IndexEntry {
        private final byte code;
        private String path;
        private final Object value;
        private final UUID timestampUuid;
        private final UUID entityId;

        public IndexEntry(UUID entityId, String path, Object value, UUID timestampUuid) {
            this.entityId = entityId;
            this.path = path;
            this.value = value;
            code = indexValueCode(value);
            this.timestampUuid = timestampUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Object getValue() {
            return value;
        }

        public byte getValueCode() {
            return code;
        }

        public UUID getTimestampUuid() {
            return timestampUuid;
        }

        public DynamicComposite getIndexComposite() {
            return new DynamicComposite(code, value, entityId, timestampUuid);
        }

      

    }
    
    
}
