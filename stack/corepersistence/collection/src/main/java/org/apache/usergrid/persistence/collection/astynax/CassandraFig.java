package org.apache.usergrid.persistence.collection.astynax;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Cassandra configuration interface.
 */
@FigSingleton
public interface CassandraFig extends GuicyFig {
    @Key( "cassandra.hosts" )
    String getHosts();

    @Key( "cassandra.version" )
    @Default( "1.2" )
    String getVersion();

    @Key( "cassandra.cluster_name" )
    @Default( "Usergrid" )
    String getClusterName();

    @Key( "collections.keyspace" )
    @Default( "Usergrid_Collections" )
    String getKeyspaceName();

    @Key( "cassandra.port" )
    @Default( "9160" )
    int getPort();

    @Key( "cassandra.connections" )
    @Default( "20" )
    int getConnections();

    @Key( "cassandra.timeout" )
    @Default( "5000" )
    int getTimeout();}
