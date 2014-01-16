package org.apache.usergrid.persistence.collection.astyanax;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import com.google.inject.Inject;
import com.google.inject.Provider;
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
public class AstyanaxKeyspaceProvider implements Provider<Keyspace> {
    private final CassandraFig cassandraConfig;

    private final static Set<AstyanaxContext<Keyspace>> contexts =
            new HashSet<AstyanaxContext<Keyspace>>();

    @Inject
    public AstyanaxKeyspaceProvider( final CassandraFig cassandraConfig ) {
        this.cassandraConfig = cassandraConfig;
    }


    @Override
    public Keyspace get() {

        AstyanaxConfiguration config = new AstyanaxConfigurationImpl()
                .setDiscoveryType( NodeDiscoveryType.TOKEN_AWARE )
                .setTargetCassandraVersion( cassandraConfig.getVersion() );

        ConnectionPoolConfiguration connectionPoolConfiguration =
                new ConnectionPoolConfigurationImpl( "UsergridConnectionPool" )
                        .setPort( cassandraConfig.getThriftPort() )
                        .setMaxConnsPerHost( cassandraConfig.getConnections() )
                        .setSeeds( cassandraConfig.getHosts() )
                        .setSocketTimeout( cassandraConfig.getTimeout() );

        AstyanaxContext<Keyspace> context =
                new AstyanaxContext.Builder().forCluster( cassandraConfig.getClusterName() )
                        .forKeyspace( cassandraConfig.getKeyspaceName() )

                        /*
                         * TODO tnine Filter this by adding a host supplier.  We will get token discovery from cassandra
                         * but only connect
                         * to nodes that have been specified.  Good for real time updates of the cass system without
                         * adding
                         * load to them during runtime
                         */

                        .withAstyanaxConfiguration( config )
                        .withConnectionPoolConfiguration( connectionPoolConfiguration )
                        .withConnectionPoolMonitor( new Slf4jConnectionPoolMonitorImpl() )
                        .buildKeyspace( ThriftFamilyFactory.getInstance() );

        context.start();
        synchronized ( contexts ) {
            contexts.add( context );
        }
        return context.getClient();
    }


    // @todo by aok - this must be considered to enable stress tests to shutdown or else
    // @todo the system will run out of file descriptors (sockets) and tests will fail.
    // this is where the lifecycle management annotations would come in handy.
    public void shutdown() {
        synchronized ( contexts ) {
            HashSet<AstyanaxContext<Keyspace>> copy = new HashSet<AstyanaxContext<Keyspace>>( contexts.size() );
            copy.addAll( contexts );

            for ( AstyanaxContext<Keyspace> context : copy ) {
                context.shutdown();
                contexts.remove( context );
            }
        }
    }
}
