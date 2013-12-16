package org.apache.usergrid.persistence.collection.cassandra;


/**
 * Cassandra configuration interface.
 */
public interface ICassandraConfig {
    /** The cassandra URL property */
    String CASSANDRA_HOSTS = "cassandra.hosts";
    String CASSANDRA_PORT = "cassandra.port";
    String CASSANDRA_CONNECTIONS = "cassandra.connections";
    String CASSANDRA_CLUSTER_NAME = "cassandra.cluster_name";
    String CASSANDRA_VERSION = "cassandra.version";
    String CASSANDRA_TIMEOUT = "cassandra.timeout";
    String COLLECTIONS_KEYSPACE_NAME = "collections.keyspace";

    String[] OPTIONS = new String[] {
            CASSANDRA_CLUSTER_NAME,
            CASSANDRA_CONNECTIONS,
            CASSANDRA_HOSTS,
            CASSANDRA_PORT,
            CASSANDRA_TIMEOUT,
            CASSANDRA_VERSION,
            COLLECTIONS_KEYSPACE_NAME
    };

    String getHosts();
    String getVersion();
    String getClusterName();
    String getKeyspaceName();
    int getPort();
    int getConnections();
    int getTimeout();}
