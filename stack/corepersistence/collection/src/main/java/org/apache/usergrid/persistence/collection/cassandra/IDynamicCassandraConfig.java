package org.apache.usergrid.persistence.collection.cassandra;


/**
 * A dynamic Cassandra configuration interface.
 */
public interface IDynamicCassandraConfig extends ICassandraConfig {
    /**
     * Registers a listener with this configuration to be notified of changes.
     *
     * @param listener the listener to register
     */
    void register( CassandraConfigListener listener );

    /**
     * Removes a registered lister from this configuration, preventing it from
     * receiving change notification events.
     *
     * @param listener the listener to unregister
     */
    void unregister( CassandraConfigListener listener );
}
