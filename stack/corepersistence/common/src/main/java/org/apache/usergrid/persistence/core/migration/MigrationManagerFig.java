package org.apache.usergrid.persistence.core.migration;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Configuration for the MigrationManager.
 */
@FigSingleton
public interface MigrationManagerFig extends GuicyFig {

    String COLLECTIONS_KEYSPACE_STRATEGY_CLASS = "collections.keyspace.strategy.class";
    String COLLECTIONS_KEYSPACE_STRATEGY_OPTIONS = "collections.keyspace.strategy.options";

    @Key(COLLECTIONS_KEYSPACE_STRATEGY_CLASS)
    @Default( "org.apache.cassandra.locator.SimpleStrategy" )
    String getStrategyClass();

    @Key(COLLECTIONS_KEYSPACE_STRATEGY_OPTIONS)
    @Default( "replication_factor:1" )
    String getStrategyOptions();

}
