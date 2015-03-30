/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.index.impl;


import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.SearchType;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.metrics.MetricsFactory;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexOperationMessage;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.map.MapManagerFactory;
import org.apache.usergrid.persistence.queue.NoAWSCredsRule;
import org.apache.usergrid.persistence.queue.QueueManagerFactory;
import org.apache.usergrid.persistence.queue.impl.UsergridAwsCredentialsProvider;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;


@RunWith(EsRunner.class)
@UseModules({ TestIndexModule.class })
@NotThreadSafe
public class BufferQueueSQSImplTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Rule
    public NoAWSCredsRule noAwsCredsRule = new NoAWSCredsRule();

    @Inject
    public QueueManagerFactory queueManagerFactory;

    @Inject
    public IndexFig indexFig;

    @Inject
    public MapManagerFactory mapManagerFactory;

    @Inject
    public MetricsFactory metricsFactory;


    private BufferQueueSQSImpl bufferQueueSQS;

    @Before
    public void setup(){
        bufferQueueSQS = new BufferQueueSQSImpl( queueManagerFactory, indexFig, mapManagerFactory, metricsFactory );
    }




    @Test
    public void testMessageIndexing(){

        ApplicationScope applicationScope = new ApplicationScopeImpl(new SimpleId(UUID.randomUUID(),"application"));
        final UsergridAwsCredentialsProvider ugProvider = new UsergridAwsCredentialsProvider();
        assumeTrue( ugProvider.getCredentials().getAWSAccessKeyId() != null );
        assumeTrue( ugProvider.getCredentials().getAWSSecretKey() != null );

        final Map<String, Object> request1Data  = new HashMap<String, Object>() {{put("test", "testval1");}};
        final IndexRequest indexRequest1 =  new IndexRequest( "testAlias1", applicationScope, SearchType.fromType("testType1"), "testDoc1",request1Data );


        final Map<String, Object> request2Data  = new HashMap<String, Object>() {{put("test", "testval2");}};
        final IndexRequest indexRequest2 =  new IndexRequest( "testAlias2", applicationScope, SearchType.fromType( "testType2"), "testDoc2",request2Data );


        //de-index request
        final DeIndexRequest deIndexRequest1 = new DeIndexRequest( new String[]{"index1.1, index1.2"}, applicationScope, new IndexScopeImpl(new SimpleId("testId3"),"name3"),  new SimpleId("id3"), UUID.randomUUID() );

        final DeIndexRequest deIndexRequest2 = new DeIndexRequest( new String[]{"index2.1", "index2.1"}, applicationScope,  new IndexScopeImpl(new SimpleId("testId4"),"name4"),  new SimpleId("id4"), UUID.randomUUID()  );




        IndexOperationMessage indexOperationMessage = new IndexOperationMessage();
        indexOperationMessage.addIndexRequest( indexRequest1);
        indexOperationMessage.addIndexRequest( indexRequest2);

        indexOperationMessage.addDeIndexRequest( deIndexRequest1 );
        indexOperationMessage.addDeIndexRequest( deIndexRequest2 );

        bufferQueueSQS.offer( indexOperationMessage );

        //wait for it to send to SQS
        indexOperationMessage.getFuture().get();

        //now get it back

        final List<IndexOperationMessage> ops = getResults( 20, TimeUnit.SECONDS );

        assertTrue(ops.size() > 0);

        final IndexOperationMessage returnedOperation = ops.get( 0 );

         //get the operations out

        final Set<IndexRequest> indexRequestSet = returnedOperation.getIndexRequests();

        assertTrue(indexRequestSet.contains(indexRequest1));
        assertTrue(indexRequestSet.contains(indexRequest2));


        final Set<DeIndexRequest> deIndexRequests = returnedOperation.getDeIndexRequests();

        assertTrue( deIndexRequests.contains( deIndexRequest1 ) );
        assertTrue( deIndexRequests.contains( deIndexRequest2 ) );



        //now ack the message

        bufferQueueSQS.ack( ops );

    }

    private List<IndexOperationMessage> getResults(final long timeout, final TimeUnit timeUnit){
        final long endTime = System.currentTimeMillis() + timeUnit.toMillis( timeout );

        List<IndexOperationMessage> ops;

        do{
            ops = bufferQueueSQS.take( 10,  20, TimeUnit.SECONDS );
        }while((ops == null || ops.size() == 0 ) &&  System.currentTimeMillis() < endTime);

        return ops;
    }




}
