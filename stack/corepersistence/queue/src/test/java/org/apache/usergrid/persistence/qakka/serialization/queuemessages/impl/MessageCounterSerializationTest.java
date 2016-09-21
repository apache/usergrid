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

package org.apache.usergrid.persistence.qakka.serialization.queuemessages.impl;

import com.google.inject.Injector;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.exceptions.NotFoundException;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.DatabaseQueueMessage;
import org.apache.usergrid.persistence.qakka.serialization.queuemessages.MessageCounterSerialization;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;


/**
 * Created by Dave Johnson (snoopdave@apache.org) on 9/20/16.
 */
public class MessageCounterSerializationTest extends AbstractTest {

    @Test
    public void testBasicOperation() {

        Injector injector = getInjector();
        MessageCounterSerialization mcs = injector.getInstance( MessageCounterSerialization.class );

        String queueName = "mcst_queue_" + RandomStringUtils.randomAlphanumeric( 20 );

        try {
            mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT );
            fail("Should have throw NotFoundException");
        } catch ( NotFoundException expected ) {
            // pass
        }

        for ( int i=0; i<10; i++ ) {
            mcs.incrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 1 );
            Assert.assertEquals( i+1, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );
        }

        mcs.decrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 0, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.incrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 10, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.incrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 20, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.incrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 30, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.incrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 40, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.incrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 50, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.incrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 50 );
        Assert.assertEquals( 100, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.decrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 90, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.decrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 80, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );

        mcs.decrementCounter( queueName, DatabaseQueueMessage.Type.DEFAULT, 10 );
        Assert.assertEquals( 70, mcs.getCounterValue( queueName, DatabaseQueueMessage.Type.DEFAULT ) );
    }

}
