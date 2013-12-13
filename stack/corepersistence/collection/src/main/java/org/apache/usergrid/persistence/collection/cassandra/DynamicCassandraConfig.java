package org.apache.usergrid.persistence.collection.cassandra;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;


/**
 */
public class DynamicCassandraConfig implements CassandraConfig {
    /** The default amount of time in milliseconds to wait before processing notification events. */
    public static final long DEFAULT_NOTIFICATION_DELAY = 1000L;

    /** The cassandra URL property */
    public static final String CASSANDRA_HOSTS = "cassandra.hosts";
    public static final String CASSANDRA_PORT = "cassandra.port";
    public static final String CASSANDRA_CONNECTIONS = "cassandra.connections";
    public static final String CASSANDRA_CLUSTER_NAME = "cassandra.cluster_name";
    public static final String CASSANDRA_VERSION = "cassandra.version";
    public static final String CASSANDRA_TIMEOUT = "cassandra.timeout";
    public static final String COLLECTIONS_KEYSPACE_NAME = "collections.keyspace";


    private DynamicStringProperty cassandraHosts;
    private String hosts;

    private DynamicIntProperty cassandraPort;
    private int port;

    private DynamicIntProperty cassandraConnections;
    private int connections;

    private DynamicIntProperty cassandraTimeout;
    private int timeout;

    private DynamicStringProperty clusterName;
    private String cluster;

    private DynamicStringProperty keyspaceName;
    private String keyspace;

    private DynamicStringProperty cassandraVersion;
    private String version;

    private Long fireTime;
    private final Set<ChangeType> changes = new HashSet<ChangeType>( ChangeType.values().length );


    // =======================================================================
    // cassandra.hosts property handling
    // =======================================================================


    public String getHosts() {
        return hosts;
    }


    @Inject
    void setCassandraHosts( @Named( CASSANDRA_HOSTS ) DynamicStringProperty cassandraHosts ) {
        this.cassandraHosts = cassandraHosts;
        this.hosts = cassandraHosts.get();

        cassandraHosts.addCallback( new Runnable() {
            @Override
            public void run() {
                notifyListeners( ChangeType.HOSTS );
            }
        } );
    }


    // =======================================================================
    // cassandra.port property handling
    // =======================================================================


    public int getPort() {
        return port;
    }


    @Inject
    void setCassandraPort( @Named( CASSANDRA_PORT ) DynamicIntProperty cassandraPort ) {
        this.cassandraPort = cassandraPort;
        this.port = cassandraPort.get();

        cassandraPort.addCallback( new Runnable() {
            @Override
            public void run() {
                notifyListeners( ChangeType.PORT );
            }
        } );
    }


    // =======================================================================
    // cassandra.connections property handling
    // =======================================================================


    public int getConnections() {
        return connections;
    }


    @Inject
    void setCassandraConnections( @Named( CASSANDRA_CONNECTIONS ) DynamicIntProperty cassandraConnections ) {
        this.cassandraConnections = cassandraConnections;
        this.connections = cassandraConnections.get();

        cassandraConnections.addCallback( new Runnable() {
            @Override
            public void run() {
                notifyListeners( ChangeType.CONNECTIONS );
            }
        } );
    }


    // =======================================================================
    // cassandra.timeout property handling
    // =======================================================================


    public int getTimeout() {
        return timeout;
    }


    @Inject
    public void setCassandraTimeout( @Named( CASSANDRA_TIMEOUT ) DynamicIntProperty cassandraTimeout ) {
        this.cassandraTimeout = cassandraTimeout;
        this.timeout = cassandraTimeout.get();

        cassandraTimeout.addCallback( new Runnable() {
            @Override
            public void run() {
                notifyListeners( ChangeType.TIMEOUT );
            }
        } );
    }


    // =======================================================================
    // cassandra.cluster_name property handling
    // =======================================================================


    public String getClusterName() {
        return cluster;
    }


    @Inject
    void setClusterName( @Named( CASSANDRA_CLUSTER_NAME ) DynamicStringProperty clusterName ) {
        this.clusterName = clusterName;
        this.cluster = clusterName.get();

        clusterName.addCallback( new Runnable() {
            @Override
            public void run() {
                notifyListeners( ChangeType.CLUSTER_NAME );
            }
        } );
    }


    // =======================================================================
    // cassandra.keyspace_name property handling
    // =======================================================================


    public String getKeyspaceName() {
        return keyspace;
    }


    @Inject
    public void setKeyspaceName( @Named( COLLECTIONS_KEYSPACE_NAME ) DynamicStringProperty keyspaceName ) {
        this.keyspaceName = keyspaceName;
        this.keyspace = keyspaceName.get();

        keyspaceName.addCallback( new Runnable() {
            @Override
            public void run() {
                notifyListeners( ChangeType.KEYSPACE_NAME );
            }
        } );
    }


    // =======================================================================
    // cassandra.version property handling
    // =======================================================================


    public String getVersion() {
        return version;
    }


    @Inject
    public void setCassandraVersion( @Named( CASSANDRA_VERSION ) DynamicStringProperty cassandraVersion ) {
        this.cassandraVersion = cassandraVersion;
        this.version = cassandraVersion.get();

        cassandraVersion.addCallback( new Runnable() {
            @Override
            public void run() {
                notifyListeners( ChangeType.VERSION );
            }
        } );
    }


    // =======================================================================
    // ===> Handling changes
    // =======================================================================


    public void register( CassandraConfigListener listener ) {

    }


    public void unregister( CassandraConfigListener listener ) {

    }


    void notifyListeners( final ChangeType hosts ) {
        synchronized ( changes ) {
            if ( fireTime == null ) {
                fireTime = System.currentTimeMillis() + DEFAULT_NOTIFICATION_DELAY;
                Thread t = new Thread( new Runnable() {
                    @Override
                    public void run() {
                        while ( fireTime != null ) {

                        }
                    }
                } );
            }
        }
    }


    public void run() {

    }


    class OldConfig implements CassandraConfig {
        final String hosts = DynamicCassandraConfig.this.getHosts();
        final String version = DynamicCassandraConfig.this.getVersion();
        final String clusterName = DynamicCassandraConfig.this.getClusterName();
        final String keyspaceName = DynamicCassandraConfig.this.getKeyspaceName();
        final int port = DynamicCassandraConfig.this.getPort();
        final int connections = DynamicCassandraConfig.this.getConnections();
        final int timeout = DynamicCassandraConfig.this.getTimeout();


        @Override
        public String getHosts() {
            return hosts;
        }


        @Override
        public String getVersion() {
            return version;
        }


        @Override
        public String getClusterName() {
            return clusterName;
        }


        @Override
        public String getKeyspaceName() {
            return keyspaceName;
        }


        @Override
        public int getPort() {
            return port;
        }


        @Override
        public int getConnections() {
            return connections;
        }


        @Override
        public int getTimeout() {
            return timeout;
        }
    }


    class NewConfig implements CassandraConfig {
        @Override
        public String getHosts() {
            return cassandraHosts.get();
        }


        @Override
        public String getVersion() {
            return cassandraVersion.get();
        }


        @Override
        public String getClusterName() {
            return clusterName.get();
        }


        @Override
        public String getKeyspaceName() {
            return keyspaceName.get();
        }


        @Override
        public int getPort() {
            return cassandraPort.get();
        }


        @Override
        public int getConnections() {
            return cassandraConnections.get();
        }


        @Override
        public int getTimeout() {
            return cassandraTimeout.get();
        }
    }
}
