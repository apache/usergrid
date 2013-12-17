package org.apache.usergrid.persistence.collection.cassandra;


import java.util.Set;


/**
 * A Cassandra configuration change event.
 */
public class CassandraConfigEvent {
    private final ICassandraConfig old;
    private final ICassandraConfig current;
    private final Set<ConfigChangeType> changes;


    CassandraConfigEvent( final ICassandraConfig old, final ICassandraConfig current, final Set<ConfigChangeType> changes ) {
        this.old = old;
        this.current = current;
        this.changes = changes;
    }


    public ICassandraConfig getOld() {
        return old;
    }


    public ICassandraConfig getCurrent() {
        return current;
    }


    public boolean hasChange( ConfigChangeType type ) {
        return changes.contains( type );
    }


    public Set<ConfigChangeType> getChanges() {
        return changes;
    }
}
