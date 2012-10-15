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
import static org.usergrid.persistence.Schema.DICTIONARY_COLLECTIONS;
import static org.usergrid.persistence.Schema.getDefaultSchema;
import static org.usergrid.persistence.cassandra.ApplicationCF.*;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addDeleteToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.addInsertToMutator;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.utils.UUIDUtils.getTimestampInMicros;
import static org.usergrid.utils.UUIDUtils.newTimeUUID;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.mutation.Mutator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.DynamicEntity;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Schema;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.cassandra.CassandraService;
import org.usergrid.persistence.cassandra.EntityManagerImpl;
import org.usergrid.persistence.cassandra.IndexUpdate;
import org.usergrid.persistence.cassandra.IndexUpdate.UniqueIndexEntry;
import org.usergrid.persistence.cassandra.RelationManagerImpl;
import org.usergrid.persistence.cassandra.IndexUpdate.IndexEntry;
import org.usergrid.persistence.schema.CollectionInfo;
import org.usergrid.utils.UUIDUtils;

/**
 * 
 * A utility to insert entities into the em for benchmarking
 * 
 * @author tnine
 * 
 */
public class EntityInsertBenchMark extends ToolBase {

    /**
     * Set to 2x your number of processors
     */
    private static final int WORKER_SIZE = 8;

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

        Options options = new Options();
        options.addOption(hostOption);
        options.addOption(countOption);
        options.addOption(appIdOption);

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

        ExecutorService executors = Executors.newFixedThreadPool(WORKER_SIZE);

        int count = Integer.parseInt(line.getOptionValue("count"));

        int size = count / WORKER_SIZE;
        
        int orphanced = count % WORKER_SIZE;

        UUID appId = UUID.fromString(line.getOptionValue("appId"));

        Stack<Future<Void>> futures = new Stack<Future<Void>>();

        for (int i = 0; i < WORKER_SIZE -1; i++) {
            futures.push(executors.submit(new InsertWorker(i, size, appId)));
        }
        
        //push a last executor with orphanced count
        futures.push(executors.submit(new InsertWorker(WORKER_SIZE-1, size+orphanced, appId)));

        System.out.println("Waiting for workers to complete insertion");

         /**
         * Wait for all tasks to complete
         */
         while(!futures.isEmpty()){
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

            SimpleEntityRef application = new SimpleEntityRef("application", appId);

            RelationManagerImpl relationManager = em.getRelationManager(application);

            for (int i = 0; i < count; i++) {

                Mutator<ByteBuffer> m = createMutator(ko, be);

                DynamicEntity dynEntity = new DynamicEntity();
                dynEntity.setType("test");
                dynEntity.setUuid(UUIDUtils.newTimeUUID());

                String value = new StringBuilder().append(workerNumber).append("-").append(i).toString();

                IndexUpdate update = new IndexUpdate(m, dynEntity, "test", value, false, false, false,
                        UUIDUtils.newTimeUUID());

                relationManager.batchUpdateCollectionIndex(update, application, "test");

                UniqueIndexer indexer = new UniqueIndexer(em.getIndexBucketLocator(), m);
                indexer.writeIndex(appId, "test", dynEntity.getUuid(), "test", value);
                // write this to the direct collection index

                m.execute();
            }

            return null;
        }

    }

    private class UniqueIndexer {

        private IndexBucketLocator indexBucketLocator;
        private Mutator<ByteBuffer> mutator;

        /**
         * @param indexBucketLocator
         * @param mutator
         */
        public UniqueIndexer(IndexBucketLocator indexBucketLocator, Mutator<ByteBuffer> mutator) {
            super();
            this.indexBucketLocator = indexBucketLocator;
            this.mutator = mutator;
        }

        private void writeIndex(UUID applicationId, String collectionName, UUID entityId, String propName,
                Object entityValue) {

            String bucketId = indexBucketLocator.getBucket(applicationId, IndexType.UNIQUE, entityId, collectionName);

            Object index_name = key(applicationId, collectionName, propName, bucketId);

            // int i = 0;

            UniqueIndexEntry entry = new UniqueIndexEntry(propName, entityValue);

            addInsertToMutator(mutator, ENTITY_UNIQUE, index_name, entry.getIndexComposite(), null,
                    System.currentTimeMillis());

        }

    }
}
