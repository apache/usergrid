package com.usergrid.count;

import com.usergrid.count.common.Count;
import me.prettyprint.cassandra.model.HCounterColumnImpl;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HCounterColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;

/**
 * Encapsulate counter writes to Cassandra
 * @author zznate
 */
public class CassandraCounterStore {

    private final Keyspace keyspace;
    private final String columnFamilyName;

    public CassandraCounterStore(Keyspace keyspace, String columnFamilyName) {
        this.keyspace = keyspace;
        this.columnFamilyName = columnFamilyName;
    }


    public void save(Count count) {
        Mutator<String> mutator = HFactory.createMutator(keyspace, StringSerializer.get());
        HCounterColumn<String> column =
                new HCounterColumnImpl<String>(count.getColumnName(), count.getValue(), StringSerializer.get());
        mutator.insertCounter(count.getKeyName(), columnFamilyName, column);
    }
}
