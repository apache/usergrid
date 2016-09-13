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

package org.apache.usergrid.persistence.qakka.serialization.transferlog;

import com.datastax.driver.core.PagingState;
import org.apache.cassandra.utils.UUIDGen;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.core.CassandraClient;
import org.apache.usergrid.persistence.qakka.core.CassandraClientImpl;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaException;
import org.apache.usergrid.persistence.qakka.serialization.Result;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class TransferLogSerializationTest extends AbstractTest {

    @Test
    public void recordTransferLog() throws Exception {

        TransferLogSerialization logSerialization = getInjector().getInstance( TransferLogSerialization.class );
        
        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        cassandraClient.getSession();

        String queueName = "tlst_queue_" + RandomStringUtils.randomAlphanumeric( 15 );
        String source = RandomStringUtils.randomAlphanumeric( 15 );
        String dest = RandomStringUtils.randomAlphanumeric( 15 );
        
        int numLogs = 100;
        
        for ( int i=0; i<numLogs; i++ ) {
            logSerialization.recordTransferLog( queueName, source, dest, UUIDGen.getTimeUUID());
        }

        int count = 0;
        int fetchCount = 0;
        PagingState pagingState = null;
        while ( true ) {
            
            Result<TransferLog> all = logSerialization.getAllTransferLogs( pagingState, 10 );
                   
            // we only want entities for our queue
            List<TransferLog> logs = all.getEntities().stream()
                .filter( log -> log.getQueueName().equals( queueName ) ).collect( Collectors.toList() );

            count += logs.size();
            fetchCount++;
            if ( all.getPagingState() == null ) {
                break;
            } 
            pagingState = all.getPagingState();
        }

        Assert.assertEquals( numLogs, count );
    }

    @Test
    public void removeTransferLog() throws Exception {

        TransferLogSerialization logSerialization = getInjector().getInstance( TransferLogSerialization.class );

        CassandraClient cassandraClient = getInjector().getInstance( CassandraClientImpl.class );
        cassandraClient.getSession(); 
        
        String queueName = "tlst_queue_" + RandomStringUtils.randomAlphanumeric( 15 );
        String source = RandomStringUtils.randomAlphanumeric( 15 );
        String dest = RandomStringUtils.randomAlphanumeric( 15 );

        UUID messageId = UUIDGen.getTimeUUID();
        logSerialization.recordTransferLog( queueName, source, dest, messageId );

        List<TransferLog> allLogs = getTransferLogs( logSerialization );

        // we only want entities for our queue
        List<TransferLog> logs = allLogs.stream()
                .filter( log -> log.getQueueName().equals( queueName ) ).collect( Collectors.toList() );
        Assert.assertEquals( 1, logs.size());

        logSerialization.removeTransferLog( queueName, source, dest, messageId );
        
        List<TransferLog> all = getTransferLogs( logSerialization );
        logs = all.stream()
            .filter( log -> log.getQueueName().equals( queueName ) ).collect( Collectors.toList() );
        Assert.assertEquals( 0, logs.size());
        
        try {
            logSerialization.removeTransferLog( queueName, source, dest, messageId );
            Assert.fail("Removing non-existent log should throw exception");
            
        } catch ( QakkaException expected ) {
            // success!
        }
    }

    private List<TransferLog> getTransferLogs(TransferLogSerialization logSerialization) {
        PagingState pagingState = null;
        List<TransferLog> allLogs = new ArrayList<>();
        while ( true ) {
            Result<TransferLog> result = logSerialization.getAllTransferLogs( pagingState, 100 );
            allLogs.addAll( result.getEntities() );
            if ( result.getPagingState() == null ) {
                break;
            }
            pagingState = result.getPagingState();
        }
        return allLogs;
    }

}