package org.apache.usergrid.persistence.collection.cassandra;


/**
 *
 */
public interface CassandraConfigListener {
    void reconfigurationEvent( CassandraEvent event );
}
