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
            HCounterColumn column =
                    new HCounterColumnImpl(count.getColumnName(), count.getValue(), count.getColumnNameSerializer());
            mutator.addCounter(count.getKeyNameBytes(), count.getTableName(), column);
          log.info("added counter: {} ", column);
        }
        try {
          mutator.execute();
        } catch (HectorException he) {
          log.error("Insert failed. Reason: ", he);
        }
    }
}
