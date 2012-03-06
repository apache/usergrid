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
package org.usergrid.mq.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static org.usergrid.mq.Message.MESSAGE_PROPERTIES;
import static org.usergrid.mq.cassandra.QueueIndexUpdate.indexValueCode;
import static org.usergrid.mq.cassandra.QueueIndexUpdate.validIndexableValue;
import static org.usergrid.mq.cassandra.QueueIndexUpdate.validIndexableValueOrJson;
import static org.usergrid.mq.cassandra.QueueManagerImpl.DICTIONARY_MESSAGE_INDEXES;
import static org.usergrid.mq.cassandra.QueuesCF.PROPERTY_INDEX;
import static org.usergrid.mq.cassandra.QueuesCF.QUEUE_DICTIONARIES;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.IndexUtils.getKeyValueList;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.DynamicCompositeSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.mutation.Mutator;

import org.usergrid.mq.Message;

public class MessageIndexUpdate {

	public static final boolean FULLTEXT = false;

	final Message message;
	final Map<String, List<Map.Entry<String, Object>>> propertyEntryList;

	public static final StringSerializer se = new StringSerializer();
	public static final ByteBufferSerializer be = new ByteBufferSerializer();
	public static final UUIDSerializer ue = new UUIDSerializer();
	public static final BytesArraySerializer bae = new BytesArraySerializer();
	public static final DynamicCompositeSerializer dce = new DynamicCompositeSerializer();
	public static final LongSerializer le = new LongSerializer();

	public MessageIndexUpdate(Message message) {
		this.message = message;

		if (message.isIndexed()) {
			propertyEntryList = new HashMap<String, List<Map.Entry<String, Object>>>();

			for (Map.Entry<String, Object> property : message.getProperties()
					.entrySet()) {

				if (!MESSAGE_PROPERTIES.containsKey(property.getKey())
						&& validIndexableValueOrJson(property.getValue())) {

					List<Map.Entry<String, Object>> list = getKeyValueList(
							property.getKey(), property.getValue(), FULLTEXT);

					propertyEntryList.put(property.getKey(), list);

				}
			}

		} else {
			propertyEntryList = null;
		}
	}

	public void addToMutation(Mutator<ByteBuffer> batch, UUID queueId,
			long shard_ts, long timestamp) {

		if (propertyEntryList != null) {
			for (Entry<String, List<Entry<String, Object>>> property : propertyEntryList
					.entrySet()) {

				for (Map.Entry<String, Object> indexEntry : property.getValue()) {

					if (validIndexableValue(indexEntry.getValue())) {

						batch.addInsertion(
								bytebuffer(key(queueId, shard_ts,
										indexEntry.getKey())),
								PROPERTY_INDEX.getColumnFamily(),
								createColumn(
										new DynamicComposite(
												indexValueCode(indexEntry
														.getValue()),
												indexEntry.getValue(), message
														.getUuid()), ByteBuffer
												.allocate(0), timestamp, dce,
										be));

						batch.addInsertion(
								bytebuffer(key(queueId,
										DICTIONARY_MESSAGE_INDEXES)),
								QUEUE_DICTIONARIES.getColumnFamily(),
								createColumn(indexEntry.getKey(),
										ByteBuffer.allocate(0), timestamp, se,
										be));
					}

				}

				batch.addInsertion(
						bytebuffer(key(queueId, DICTIONARY_MESSAGE_INDEXES)),
						QUEUE_DICTIONARIES.getColumnFamily(),
						createColumn(property.getKey(), ByteBuffer.allocate(0),
								timestamp, se, be));

			}

		}

	}

	public Message getMessage() {
		return message;
	}

}
