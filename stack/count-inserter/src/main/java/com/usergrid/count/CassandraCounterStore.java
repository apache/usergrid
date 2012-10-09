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
package com.usergrid.count;

import com.usergrid.count.common.Count;
import me.prettyprint.cassandra.model.HCounterColumnImpl;
import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.exceptions.HectorException;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

/**
 * Encapsulate counter writes to Cassandra
 * @author zznate
 */
public class CassandraCounterStore implements CounterStore {
    private Logger log = LoggerFactory.getLogger(CassandraCounterStore.class);

    private final Keyspace keyspace;

    public CassandraCounterStore(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    public void save(Count count) {
        this.save(Arrays.asList(count));
    }

    public void save(Collection<Count> counts) {
        Mutator<ByteBuffer> mutator = HFactory.createMutator(keyspace, ByteBufferSerializer.get());
        for ( Count count : counts ) {
            mutator.addCounter(count.getKeyNameBytes(), count.getTableName(),
                    new HCounterColumnImpl(count.getColumnName(), count.getValue(), count.getColumnNameSerializer()));
        }
        try {
          mutator.execute();
        } catch (HectorException he) {
          log.error("Insert failed. Reason: ", he);
        }
    }
}
