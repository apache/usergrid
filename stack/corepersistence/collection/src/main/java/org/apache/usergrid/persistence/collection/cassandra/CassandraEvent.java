package org.apache.usergrid.persistence.collection.cassandra;


import java.util.Set;


/**
 *
 */
public class CassandraEvent {
    private final CassandraConfig old;
    private final CassandraConfig current;
    private final Set<ChangeType> changes;


    CassandraEvent( final CassandraConfig old, final CassandraConfig current, final Set<ChangeType> changes ) {
        this.old = old;
        this.current = current;
        this.changes = changes;
    }


    public CassandraConfig getOld() {
        return old;
    }


    public CassandraConfig getCurrent() {
        return current;
    }


    public boolean hasChange( ChangeType type ) {
        return changes.contains( type );
    }
}
