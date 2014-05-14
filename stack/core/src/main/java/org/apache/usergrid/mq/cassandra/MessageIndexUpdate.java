/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.mq.cassandra;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.usergrid.mq.Message;

import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.mutation.Mutator;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static org.apache.usergrid.mq.Message.MESSAGE_PROPERTIES;
import static org.apache.usergrid.mq.cassandra.QueueIndexUpdate.indexValueCode;
import static org.apache.usergrid.mq.cassandra.QueueIndexUpdate.validIndexableValue;
import static org.apache.usergrid.mq.cassandra.QueueIndexUpdate.validIndexableValueOrJson;
import static org.apache.usergrid.mq.cassandra.QueueManagerImpl.DICTIONARY_MESSAGE_INDEXES;
import static org.apache.usergrid.mq.cassandra.QueuesCF.PROPERTY_INDEX;
import static org.apache.usergrid.mq.cassandra.QueuesCF.QUEUE_DICTIONARIES;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.IndexUtils.getKeyValueList;
import static org.apache.usergrid.persistence.cassandra.Serializers.*;


public class MessageIndexUpdate {

    public static final boolean FULLTEXT = false;

    final Message message;
    final Map<String, List<Map.Entry<String, Object>>> propertyEntryList;

    public MessageIndexUpdate( Message message ) {
        this.message = message;

        if ( message.isIndexed() ) {
            propertyEntryList = new HashMap<String, List<Map.Entry<String, Object>>>();

            for ( Map.Entry<String, Object> property : message.getProperties().entrySet() ) {

                if ( !MESSAGE_PROPERTIES.containsKey( property.getKey() ) && validIndexableValueOrJson(
                        property.getValue() ) ) {

                    List<Map.Entry<String, Object>> list =
                            getKeyValueList( property.getKey(), property.getValue(), FULLTEXT );

                    propertyEntryList.put( property.getKey(), list );
                }
            }
        }
        else {
            propertyEntryList = null;
        }
    }


    public void addToMutation( Mutator<ByteBuffer> batch, UUID queueId, long shard_ts, long timestamp ) {

        if ( propertyEntryList != null ) {
            for ( Entry<String, List<Entry<String, Object>>> property : propertyEntryList.entrySet() ) {

                for ( Map.Entry<String, Object> indexEntry : property.getValue() ) {

                    if ( validIndexableValue( indexEntry.getValue() ) ) {

                        batch.addInsertion( bytebuffer( key( queueId, shard_ts, indexEntry.getKey() ) ),
                                PROPERTY_INDEX.getColumnFamily(), createColumn(
                                new DynamicComposite( indexValueCode( indexEntry.getValue() ), indexEntry.getValue(),
                                        message.getUuid() ), ByteBuffer.allocate( 0 ), timestamp, dce, be ) );

                        batch.addInsertion( bytebuffer( key( queueId, DICTIONARY_MESSAGE_INDEXES ) ),
                                QUEUE_DICTIONARIES.getColumnFamily(),
                                createColumn( indexEntry.getKey(), ByteBuffer.allocate( 0 ), timestamp, se, be ) );
                    }
                }

                batch.addInsertion( bytebuffer( key( queueId, DICTIONARY_MESSAGE_INDEXES ) ),
                        QUEUE_DICTIONARIES.getColumnFamily(),
                        createColumn( property.getKey(), ByteBuffer.allocate( 0 ), timestamp, se, be ) );
            }
        }
    }


    public Message getMessage() {
        return message;
    }
}
