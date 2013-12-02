package org.apache.usergrid.persistence.collection.astynax;


import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.netflix.astyanax.AstyanaxConfiguration;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.ConnectionPoolConfiguration;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.Slf4jConnectionPoolMonitorImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;


/**
 * TODO.  Provide the ability to do a service hook for realtime tuning without the need of a JVM restart This could be
 * done with governator and service discovery
 *
 * @author tnine
 */
public class AstynaxKeyspaceProvider implements Provider<Keyspace> {

    /** The cassandra URL property */
    public static final String CASSANDRA_HOSTS = "cassandra.hosts";
    public static final String CASSANDRA_PORT = "cassandra.port";
    public static final String CASSANDRA_CONNECTIONS = "cassandra.connections";
    public static final String CASSANDRA_CLUSTER_NAME = "cassandra.cluster_name";
    public static final String CASSANDRA_VERSION = "cassandra.version";
    public static final String CASSANDRA_TIMEOUT = "cassandra.timeout";
    public static final String COLLECTIONS_KEYSPACE_NAME = "collections.keyspace";


    protected final String cassandraHosts;
    protected final int cassandraPort;
    protected final int cassandraConnections;
    protected final int cassandraTimeout;
    protected final String clusterName;
    protected final String keyspaceName;
    protected final String cassandraVersion;


    @Inject
    public AstynaxKeyspaceProvider( @Named(CASSANDRA_HOSTS) final String cassandraHosts,
                                    @Named(CASSANDRA_PORT) final int cassandraPort,
                                    @Named(CASSANDRA_CONNECTIONS) final int cassandraConnections,
                                    @Named(CASSANDRA_CLUSTER_NAME) final String clusterName,
                                    @Named(CASSANDRA_VERSION) final String cassandraVersion,
                                    @Named(COLLECTIONS_KEYSPACE_NAME) final String keyspaceName,
                                    @Named( CASSANDRA_TIMEOUT ) final int cassandraTimeout ) {
        this.cassandraHosts = cassandraHosts;
        this.cassandraPort = cassandraPort;
        this.cassandraConnections = cassandraConnections;
        this.cassandraTimeout = cassandraTimeout;
        this.clusterName = clusterName;
        this.keyspaceName = keyspaceName;
        this.cassandraVersion = cassandraVersion;
    }


    @Override
    public Keyspace get() {
        AstyanaxConfiguration config = new AstyanaxConfigurationImpl().setDiscoveryType( NodeDiscoveryType.TOKEN_AWARE )
                                                                      .setTargetCassandraVersion( cassandraVersion );

        ConnectionPoolConfiguration connectionPoolConfiguration =
                new ConnectionPoolConfigurationImpl( "UsergridConnectionPool" ).setPort( cassandraPort )
                                                                               .setMaxConnsPerHost(
                                                                                       cassandraConnections )
                                                                               .setSeeds( cassandraHosts )
                                                                               .setSocketTimeout( cassandraTimeout );

        AstyanaxContext<Keyspace> context =
                new AstyanaxContext.Builder().forCluster( clusterName ).forKeyspace( keyspaceName )
                        /**
                         *TODO tnine Filter this by adding a host supplier.  We will get token discovery from cassandra
                         * but only connect
                         * to nodes that have been specified.  Good for real time updates of the cass system without
                         * adding
                         * load to them during runtime
                         */.withAstyanaxConfiguration( config )
                           .withConnectionPoolConfiguration( connectionPoolConfiguration )
                           .withConnectionPoolMonitor( new Slf4jConnectionPoolMonitorImpl() )
                           .buildKeyspace( ThriftFamilyFactory.getInstance() );

        context.start();


        return context.getClient();
    }


    /**
     * Get runtime options that can be overridden.  TODO: Make this an interface and somehow hook it into Guice
     * auotmagically
     */
    public static String[] getRuntimeOptions() {
        return new String[] {
                CASSANDRA_HOSTS, CASSANDRA_PORT, CASSANDRA_CONNECTIONS, CASSANDRA_CLUSTER_NAME, CASSANDRA_VERSION,
                COLLECTIONS_KEYSPACE_NAME
        };
    }
}
