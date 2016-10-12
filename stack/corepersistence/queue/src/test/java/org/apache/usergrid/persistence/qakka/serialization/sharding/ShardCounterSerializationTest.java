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

package org.apache.usergrid.persistence.qakka.serialization.sharding;

import com.google.inject.Injector;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.exceptions.NotFoundException;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;


public class ShardCounterSerializationTest extends AbstractTest {


    @Test
    public void testBasicOperation() throws Exception {

        ShardCounterSerialization scs = getInjector().getInstance( ShardCounterSerialization.class );

        String queueName = "scst_queue_" + RandomStringUtils.randomAlphanumeric( 20 );
        long shardId = 100L;

        try {
            scs.getCounterValue( queueName, Shard.Type.DEFAULT, shardId );
            fail("Should have throw NotFoundException");
        } catch ( NotFoundException expected ) {
            // pass
        }

        scs.incrementCounter( queueName, Shard.Type.DEFAULT, shardId, 10 );
        Assert.assertEquals( 10, scs.getCounterValue( queueName, Shard.Type.DEFAULT, shardId ) );

        scs.incrementCounter( queueName, Shard.Type.DEFAULT, shardId, 50 );
        Assert.assertEquals( 60, scs.getCounterValue( queueName, Shard.Type.DEFAULT, shardId ) );

        scs.incrementCounter( queueName, Shard.Type.DEFAULT, shardId, 150 );
        Assert.assertEquals( 210, scs.getCounterValue( queueName, Shard.Type.DEFAULT, shardId ) );
    }


    @Test
    public void testConcurrentOperation() {

        // create multiple threads, each will increment counter by some number

        Injector injector = getInjector();
        ShardCounterSerialization scs = injector.getInstance( ShardCounterSerialization.class );
        String queueName = "stco_queue_" + RandomStringUtils.randomAlphanumeric( 10 );
        long shardId = 100L;

        int poolSize = 20;
        int numThreads = 20;
        int numCounts = 3000;
        ExecutorService execService = Executors.newFixedThreadPool( poolSize );

        for (int i = 0; i < numThreads; i++) {

            execService.submit( () -> {

                for ( int j = 0; j < numCounts; j++ ) {
                    scs.incrementCounter( queueName, Shard.Type.DEFAULT, shardId, 1 );
                }

            });
        }

        execService.shutdown();

        try {
            while (!execService.awaitTermination( 3, TimeUnit.SECONDS )) {
                System.out.println( "Waiting... " +
                    scs.getCounterValue( queueName, Shard.Type.DEFAULT, shardId )  );
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // test that counter is correct value

        Assert.assertEquals( numThreads * numCounts,
            scs.getCounterValue( queueName, Shard.Type.DEFAULT, shardId ) );
    }

}
