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

package org.apache.usergrid.persistence.qakka.serialization.queuemessages;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.persistence.qakka.AbstractTest;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaRuntimeException;
import org.apache.usergrid.persistence.qakka.serialization.sharding.Shard;
import org.apache.usergrid.persistence.qakka.core.QakkaUtils;
import org.junit.Test;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class DatabaseQueueMessageSerializationTest extends AbstractTest {


    static class ThingToSave implements Serializable {
        String value;
    }


    @Test
    public void writeNewMessage(){

        QueueMessageSerialization queueMessageSerialization =
                getInjector().getInstance( QueueMessageSerialization.class );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);

        DatabaseQueueMessage message1 = new DatabaseQueueMessage(QakkaUtils.getTimeUuid(),
                DatabaseQueueMessage.Type.DEFAULT, "test", "region1",
                shard1.getShardId(), System.currentTimeMillis(), null, null);

        UUID queueMessageId = queueMessageSerialization.writeMessage(message1);
    }

    @Test
    public void deleteMessage(){

        QueueMessageSerialization queueMessageSerialization =
                getInjector().getInstance( QueueMessageSerialization.class );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);

        UUID messageId = QakkaUtils.getTimeUuid();
        String queueName = "dqmst_queue_" + RandomStringUtils.randomAlphanumeric( 20 );

        DatabaseQueueMessage message = new DatabaseQueueMessage(
                messageId,
                DatabaseQueueMessage.Type.DEFAULT,
                queueName,
                "dummy_region",
                shard1.getShardId(),
                System.currentTimeMillis(),
                null, null );

        UUID queueMessageId = queueMessageSerialization.writeMessage( message );

        queueMessageSerialization.deleteMessage(
            queueName,
            "dummy_region",
            shard1.getShardId(),
            DatabaseQueueMessage.Type.DEFAULT,
            queueMessageId );

        assertNull( queueMessageSerialization.loadMessage(
            queueName,
            "dummy_region",
            shard1.getShardId(),
            DatabaseQueueMessage.Type.DEFAULT,
            queueMessageId
        ));
    }


    @Test
    public void loadNullMessage(){

        QueueMessageSerialization queueMessageSerialization =
                getInjector().getInstance( QueueMessageSerialization.class );

        Shard shard1 = new Shard("junk", "region1", Shard.Type.DEFAULT, 100L, null);

        assertNull( queueMessageSerialization.loadMessage(
                RandomStringUtils.randomAlphanumeric( 20 ),
                "dummy_region",
                shard1.getShardId(),
                DatabaseQueueMessage.Type.DEFAULT,
                null
        ));
    }


    @Test
    public void writeNewMessageData(){

        QueueMessageSerialization queueMessageSerialization =
                getInjector().getInstance( QueueMessageSerialization.class );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);

        UUID messageId = QakkaUtils.getTimeUuid();

        final String data = "my test data";

        final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody(
                DataType.serializeValue(data, ProtocolVersion.NEWEST_SUPPORTED), "text/plain");

        queueMessageSerialization.writeMessageData(messageId, messageBody);

        final DatabaseQueueMessageBody returnedData = queueMessageSerialization.loadMessageData( messageId );
    }


    @Test
    public void loadMessageData() throws Exception {

        QueueMessageSerialization queueMessageSerialization =
                getInjector().getInstance( QueueMessageSerialization.class );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);

        UUID messageId = QakkaUtils.getTimeUuid();

        final String data = "my test data";

        final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody( DataType.serializeValue(data,
                ProtocolVersion.NEWEST_SUPPORTED), "text/plain");

        queueMessageSerialization.writeMessageData(messageId, messageBody);

        final DatabaseQueueMessageBody returnedBody = queueMessageSerialization.loadMessageData( messageId );
        String returnedData = new String( returnedBody.getBlob().array(), "UTF-8");

        assertEquals(data, returnedData);
    }


    @Test
    public void loadMessageObjectData() throws Exception {

        QueueMessageSerialization queueMessageSerialization =
            getInjector().getInstance( QueueMessageSerialization.class );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);

        UUID messageId = QakkaUtils.getTimeUuid();

        final String data = "my test data";

        final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody( DataType.serializeValue(data,
            ProtocolVersion.NEWEST_SUPPORTED), "text/plain");

        queueMessageSerialization.writeMessageData(messageId, messageBody);

        final DatabaseQueueMessageBody returnedBody = queueMessageSerialization.loadMessageData( messageId );
        String returnedData = new String( returnedBody.getBlob().array(), "UTF-8");

        assertEquals(data, returnedData);
    }




    @Test
    public void deleteMessageData() throws UnsupportedEncodingException {

        QueueMessageSerialization queueMessageSerialization =
                getInjector().getInstance( QueueMessageSerialization.class );

        Shard shard1 = new Shard("test", "region1", Shard.Type.DEFAULT, 100L, null);

        UUID messageId = QakkaUtils.getTimeUuid();

        final String data = "my test data";

        final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody( DataType.serializeValue(data,
                ProtocolVersion.NEWEST_SUPPORTED), "text/plain");

        queueMessageSerialization.writeMessageData(messageId, messageBody);

        final DatabaseQueueMessageBody returnedBody = queueMessageSerialization.loadMessageData( messageId );
        final String returnedData = new String( returnedBody.getBlob().array(), "UTF-8");

        assertEquals(data, returnedData);

        queueMessageSerialization.deleteMessageData(messageId);

        assertNull(queueMessageSerialization.loadMessageData( messageId ));


    }


    /**
     * Persist to blob using Java serialization.
     */
    @Test
    public void persistJavaObjectData() throws Exception {

        QueueMessageSerialization queueMessageSerialization =
            getInjector().getInstance( QueueMessageSerialization.class );

        // serialize Java object to byte buffer

        final ThingToSave data = new ThingToSave();
        data.value = "my test data";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(data);
        oos.flush();
        oos.close();
        ByteBuffer byteBuffer = ByteBuffer.wrap( bos.toByteArray() );

        // write to Cassandra

        final DatabaseQueueMessageBody messageBody = new DatabaseQueueMessageBody(
            byteBuffer,"application/octet-stream");

        UUID messageId = QakkaUtils.getTimeUuid();
        queueMessageSerialization.writeMessageData(messageId, messageBody);

        // load from Cassandra

        final DatabaseQueueMessageBody returnedBody = queueMessageSerialization.loadMessageData( messageId );

        // deserialize byte buffer

        ByteBuffer messageData = returnedBody.getBlob();
        ByteArrayInputStream bais = new ByteArrayInputStream( messageData.array() );

        // throws exception -> java.io.StreamCorruptedException: invalid stream header: 00000000
        ObjectInputStream ios = new ObjectInputStream( bais );
        ThingToSave returnedData = (ThingToSave)ios.readObject();

        assertEquals( data.value, returnedData.value );
    }

}
