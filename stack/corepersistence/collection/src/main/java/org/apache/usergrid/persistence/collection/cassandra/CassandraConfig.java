package org.apache.usergrid.persistence.collection.cassandra;


/**
 * Cassandra configuration interface.
 */
public interface CassandraConfig {
    String getHosts();
    String getVersion();
    String getClusterName();
    String getKeyspaceName();
    int getPort();
    int getConnections();
    int getTimeout();
}
