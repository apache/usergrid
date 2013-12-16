package org.apache.usergrid.persistence.collection.cassandra;


/**
 * The kinds of Cassandra configuration changes that might take place.
 */
public enum ConfigChangeType {
    HOSTS, CLUSTER_NAME, KEYSPACE_NAME, PORT, CONNECTIONS, TIMEOUT, VERSION
}
