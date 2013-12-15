package org.apache.usergrid.persistence.collection.cassandra;


import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicStringProperty;


/**
 * A dynamic ICassandraConfig implementation that allows registration by
 * listeners for configuration change events.
 */
@Singleton
public class DynamicCassandraConfig implements IDynamicCassandraConfig {
    private static final Logger LOG = LoggerFactory.getLogger( DynamicCassandraConfig.class );

    /** The default amount of time in milliseconds to wait before processing notification events. */
    public static final long DEFAULT_NOTIFICATION_DELAY = 1000L;

    private DynamicStringProperty dynamicHosts;
    private String hosts;

    private DynamicIntProperty dynamicPort;
    private int port;

    private DynamicIntProperty dynamicConnections;
    private int connections;

    private DynamicIntProperty dynamicTimeout;
    private int timeout;

    private DynamicStringProperty dynamicCluster;
    private String cluster;

    private DynamicStringProperty dynamicKeyspace;
    private String keyspace;

    private DynamicStringProperty dynamicVersion;
    private String version;

    private Long fireTime;
    private final Set<ConfigChangeType> changes = new HashSet<ConfigChangeType>( ConfigChangeType.values().length );
    private final LinkedList<CassandraConfigListener> listeners = new LinkedList<CassandraConfigListener>();


    // =======================================================================
    // cassandra.hosts property handling
    // =======================================================================


    public String getHosts() {
        return hosts;
    }


    @Inject
    void setDynamicHosts( @Named( CASSANDRA_HOSTS ) DynamicStringProperty cassandraHosts ) {
        this.dynamicHosts = cassandraHosts;
        this.hosts = cassandraHosts.get();

        cassandraHosts.addCallback( new Runnable() {
            @Override
            public void run() {
                queueNotifications( ConfigChangeType.HOSTS );
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
    void setDynamicPort( @Named( CASSANDRA_PORT ) DynamicIntProperty cassandraPort ) {
        this.dynamicPort = cassandraPort;
        this.port = cassandraPort.get();

        cassandraPort.addCallback( new Runnable() {
            @Override
            public void run() {
                queueNotifications( ConfigChangeType.PORT );
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
    void setDynamicConnections( @Named( CASSANDRA_CONNECTIONS ) DynamicIntProperty cassandraConnections ) {
        this.dynamicConnections = cassandraConnections;
        this.connections = cassandraConnections.get();

        cassandraConnections.addCallback( new Runnable() {
            @Override
            public void run() {
                queueNotifications( ConfigChangeType.CONNECTIONS );
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
    public void setDynamicTimeout( @Named( CASSANDRA_TIMEOUT ) DynamicIntProperty cassandraTimeout ) {
        this.dynamicTimeout = cassandraTimeout;
        this.timeout = cassandraTimeout.get();

        cassandraTimeout.addCallback( new Runnable() {
            @Override
            public void run() {
                queueNotifications( ConfigChangeType.TIMEOUT );
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
        this.dynamicCluster = clusterName;
        this.cluster = clusterName.get();

        clusterName.addCallback( new Runnable() {
            @Override
            public void run() {
                queueNotifications( ConfigChangeType.CLUSTER_NAME );
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
        this.dynamicKeyspace = keyspaceName;
        this.keyspace = keyspaceName.get();

        keyspaceName.addCallback( new Runnable() {
            @Override
            public void run() {
                queueNotifications( ConfigChangeType.KEYSPACE_NAME );
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
    public void setDynamicVersion( @Named( CASSANDRA_VERSION ) DynamicStringProperty cassandraVersion ) {
        this.dynamicVersion = cassandraVersion;
        this.version = cassandraVersion.get();

        cassandraVersion.addCallback( new Runnable() {
            @Override
            public void run() {
                queueNotifications( ConfigChangeType.VERSION );
            }
        } );
    }


    // =======================================================================
    // ===> Handling changes
    // =======================================================================


    public void register( CassandraConfigListener listener ) {
        synchronized ( listeners ) {
            listeners.addLast( listener );
        }
    }


    public void unregister( CassandraConfigListener listener ) {
        synchronized ( listeners ) {
            listeners.remove( listener );
        }
    }


    /**
     * This method queues up changes in the event several changes come in at
     * once (which may often be the case) before firing off notifications to
     * listeners. It will keep delaying the time to fire notifications if
     * changes continue to come in by the default delay time. Only when the
     * last change in a train of changes has come in, and the default delay
     * has been hit does it notify all listeners with the summary of changes.
     *
     * NOTE: (aok) was going to use a Timer or TimerTask for this but this
     * happens so infrequently that it's not a big deal to create the new
     * thread and use it on the spot - better than keeping it around IMHO.
     *
     * @param change the change to queue up for notification to listeners
     */
    private void queueNotifications( final ConfigChangeType change ) {
        synchronized ( changes ) {
            if ( fireTime == null ) {
                assert changes.isEmpty();
                changes.add( change );
                fireTime = System.currentTimeMillis() + DEFAULT_NOTIFICATION_DELAY;
                new Thread( new Runnable() {
                    @Override
                    public void run() {
                        long timeToWait = 1;
                        synchronized ( changes ) {
                            while ( fireTime != null && timeToWait > 0 ) {
                                timeToWait = fireTime - System.currentTimeMillis();
                                if ( timeToWait <= 0 ) {
                                    break;
                                }

                                try {
                                    changes.wait( timeToWait );
                                    changes.notifyAll();
                                }
                                catch ( InterruptedException e ) {
                                    LOG.error( "Awe snap, someone wake me up before it was time." );
                                }
                            }

                            // now we can really notify listeners, clean up, and die
                            notifyListeners( Collections.unmodifiableSet( new HashSet<ConfigChangeType>( changes ) ) );
                            applyNewValues();
                            changes.clear();
                            fireTime = null;
                        }
                    }
                } ).start();
            }
            else {
                fireTime = System.currentTimeMillis() + DEFAULT_NOTIFICATION_DELAY;
                changes.add( change );
            }
        }
    }


    @SuppressWarnings( "ConstantConditions" )
    private void applyNewValues() {
        for ( ConfigChangeType change : changes ) {
            switch ( change ) {
                case HOSTS:
                    this.hosts = dynamicHosts.get();
                    break;
                case CLUSTER_NAME:
                    this.cluster = dynamicCluster.get();
                    break;
                case KEYSPACE_NAME:
                    this.keyspace = dynamicKeyspace.get();
                    break;
                case PORT:
                    this.port = dynamicPort.get();
                    break;
                case CONNECTIONS:
                    this.connections = dynamicConnections.get();
                    break;
                case TIMEOUT:
                    this.timeout = dynamicTimeout.get();
                    break;
                case VERSION:
                    this.version = dynamicVersion.get();
                    break;
                default:
                    throw new RuntimeException( "Should never get here!" );
            }
        }
    }


    private void notifyListeners( Set<ConfigChangeType> changes ) {
        // (0) lock on listeners
        // (1) create final new and old config objects
        // (2) build the event with them
        // (3) iterate through listeners and call their notification method

        synchronized ( listeners ) {
            final NewConfig newConfig = new NewConfig();
            final OldConfig oldConfig = new OldConfig(); // new Old confusing eh? hahaha
            final CassandraConfigEvent event = new CassandraConfigEvent( oldConfig, newConfig, changes );
            for ( CassandraConfigListener listener : listeners ) {
                listener.reconfigurationEvent( event );
            }
        }
    }


    class OldConfig implements ICassandraConfig {
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


    class NewConfig implements ICassandraConfig {
        @Override
        public String getHosts() {
            return dynamicHosts.get();
        }


        @Override
        public String getVersion() {
            return dynamicVersion.get();
        }


        @Override
        public String getClusterName() {
            return dynamicCluster.get();
        }


        @Override
        public String getKeyspaceName() {
            return dynamicKeyspace.get();
        }


        @Override
        public int getPort() {
            return dynamicPort.get();
        }


        @Override
        public int getConnections() {
            return dynamicConnections.get();
        }


        @Override
        public int getTimeout() {
            return dynamicTimeout.get();
        }
    }
}
