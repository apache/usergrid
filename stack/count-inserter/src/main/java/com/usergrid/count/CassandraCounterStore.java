package com.usergrid.count;

import com.usergrid.count.common.Count;
import me.prettyprint.cassandra.model.HCounterColumnImpl;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;

import java.util.Arrays;
import java.util.List;

/**
 * Encapsulate counter writes to Cassandra
 * @author zznate
 */
public class CassandraCounterStore implements CounterStore {

    private final Keyspace keyspace;

    public CassandraCounterStore(Keyspace keyspace) {
        this.keyspace = keyspace;
    }

    public void save(Count count) {
        this.save(Arrays.asList(count));
    }

    public void save(List<Count> counts) {
        Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
        for ( Count count : counts ) {
            HCounterColumn<String> column =
                    new HCounterColumnImpl<String>(count.getColumnName(), count.getValue(), StringSerializer.get());
            mutator.addCounter(count.getKeyName(), count.getTableName(), column);
        }
        mutator.execute();
    }
}
