package org.apache.usergrid.persistence.collection.cassandra;


/**
 * Cassandra configuration change listener interface.
 */
public interface CassandraConfigListener {
    void reconfigurationEvent( CassandraConfigEvent event );
}
