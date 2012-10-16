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
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_INDEX;
import static org.usergrid.persistence.cassandra.ApplicationCF.ENTITY_UNIQUE;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.persistence.cassandra.IndexUpdate.indexValueCode;
import static org.usergrid.utils.ConversionUtils.bytebuffers;
import static org.usergrid.utils.ConversionUtils.bytes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.DynamicCompositeSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.query.MultigetSliceQuery;
import me.prettyprint.hector.api.query.QueryResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.usergrid.persistence.IndexBucketLocator;
import org.usergrid.persistence.IndexBucketLocator.IndexType;
import org.usergrid.persistence.cassandra.EntityManagerImpl;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricPredicate;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import com.yammer.metrics.reporting.ConsoleReporter;

/**
 * 
 * A utility to insert entities into the em for benchmarking
 * 
 * @author tnine
 * 
 */
public class EntityReadBenchMark extends ToolBase {

    /**
     * Set to 2x your number of processors
     */
    private static final int WORKER_SIZE = 8;

    public static final ByteBufferSerializer be = new ByteBufferSerializer();

    private static final Logger logger = LoggerFactory.getLogger(EntityReadBenchMark.class);

    private final Timer queryReads = Metrics.newTimer(ReadWorker.class, "entity", TimeUnit.MILLISECONDS,
            TimeUnit.SECONDS);

    private final Timer dictReads = Metrics
            .newTimer(ReadWorker.class, "dictionary", TimeUnit.MILLISECONDS, TimeUnit.SECONDS);

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

        UUID appId = UUID.fromString(line.getOptionValue("appId"));

        System.out.println("Querying unique properties in the search index");

        
        final ConsoleReporter reporter = new ConsoleReporter(Metrics.defaultRegistry(),
                System.out,
                MetricPredicate.ALL);
        
        //print every 30 seconds
        reporter.start(30, TimeUnit.SECONDS);
        
        Stack<Future<Void>> futures = new Stack<Future<Void>>();

                
//        for (int i = 0; i < WORKER_SIZE; i++) {
//            futures.push(executors.submit(new IndexReadWorker(i, size, appId)));
//        }
        
    
        System.out.println("Waiting for index read workers to complete");

        /**
         * Wait for all tasks to complete
         */
        while (!futures.isEmpty()) {
            futures.pop().get();
        }
        
      
       
        
        for (int i = 0; i < WORKER_SIZE; i++) {
            futures.push(executors.submit(new DictReadWorker(i, size, appId)));
        }
        
        /**
         * Wait for all tasks to complete
         */
        while (!futures.isEmpty()) {
            futures.pop().get();
        }
        

        System.out.println("All workers completed reading");
        
        
        
        //print the report
        reporter.run();

    }

    private abstract class  ReadWorker implements Callable<Void> {

        protected int count;

        protected int workerNumber;

        protected UUID appId;

        
        private ReadWorker(int workerNumber, int count, UUID appId) throws Exception {
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

           
            for (int i = 0; i < count; i++) {

                String value = new StringBuilder().append(workerNumber).append("-").append(i).toString();

               
               doRead(value);

            }

            return null;
        }
        
        protected abstract void doRead(String value) throws Exception;

    }
    
    private class IndexReadWorker extends ReadWorker{

        private Keyspace keyspace;
        private IndexBucketLocator indexBucketLocator = null;
        
        private IndexReadWorker(int workerNumber, int count, UUID appId) throws Exception {
           super(workerNumber, count, appId);
           keyspace = EntityReadBenchMark.this.cass.getApplicationKeyspace(appId);
           indexBucketLocator =  ((EntityManagerImpl)EntityReadBenchMark.this.emf.getEntityManager(appId)).getIndexBucketLocator();
        }
        
        /* (non-Javadoc)
         * @see org.usergrid.tools.EntityReadBenchMark.ReadWorker#doRead()
         */
        @Override
        protected void doRead(String value) throws Exception {
            TimerContext timer = queryReads.time();
         
            Assert.isTrue(read(value));
            
            timer.stop();
            
        }
        
        private boolean read(String value){

            
            List<String> buckets = indexBucketLocator.getBuckets(appId, IndexType.UNIQUE, "tests");

            List<Object> cassKeys = new ArrayList<Object>(buckets.size());

            Object keyPrefix = key(appId, "tests", "test");

            for (String bucket : buckets) {
                cassKeys.add(key(keyPrefix, bucket));
            }

            MultigetSliceQuery<ByteBuffer, DynamicComposite, ByteBuffer> multiget = HFactory.createMultigetSliceQuery(
                    keyspace, be, DynamicCompositeSerializer.get(),
                    ByteBufferSerializer.get());

            multiget.setColumnFamily(ENTITY_INDEX.getColumnFamily());
            multiget.setKeys(bytebuffers(cassKeys));

            
            
            
            DynamicComposite start = new DynamicComposite(indexValueCode(value), value);
            
            DynamicComposite finish = new DynamicComposite(indexValueCode(value));
            finish.addComponent(1, value, ComponentEquality.GREATER_THAN_EQUAL);
            
            
            multiget.setRange(start, finish, false, 1);
            QueryResult<Rows<ByteBuffer, DynamicComposite, ByteBuffer>> results = multiget.execute();

            // search for a column, if one exists, we've found the entity
            for (Row<ByteBuffer, DynamicComposite, ByteBuffer> row : results.get()) {
                if (row.getColumnSlice().getColumns().size() > 0) {
                    return true;
                }
            }

            return false;
        }
        
    }
    
    private class DictReadWorker extends ReadWorker{


        UniqueIndexer indexer;
        

        private DictReadWorker(int workerNumber, int count, UUID appId) throws Exception {
           super(workerNumber, count, appId);
           Keyspace ko = EntityReadBenchMark.this.cass.getApplicationKeyspace(appId);
           indexer = new UniqueIndexer(ko);
        }
        
        /* (non-Javadoc)
         * @see org.usergrid.tools.EntityReadBenchMark.ReadWorker#doRead()
         */
        @Override
        protected void doRead(String value) throws Exception {

            TimerContext timer = dictReads.time();
            
            Assert.isTrue(indexer.existsInIndex(appId, "tests", "test", value));
            
            timer.stop();
            
            
            

        }
        
    }

    private class UniqueIndexer {

        private Keyspace keyspace;

        /**
         * @param indexBucketLocator
         * @param mutator
         */
        public UniqueIndexer(Keyspace keyspace) {
            super();
            this.keyspace = keyspace;
        }

        private boolean existsInIndex(UUID applicationId, String collectionName, String propName, Object entityValue) throws Exception {
            Object rowKey = key(applicationId, collectionName, propName, md5(bytes(entityValue)));

            
            List<HColumn<ByteBuffer, ByteBuffer>> cols = cass.getColumns(keyspace, ENTITY_UNIQUE, rowKey, null, null, 2, false);
           
            
            return cols.size() > 0;

        }
    }
}
