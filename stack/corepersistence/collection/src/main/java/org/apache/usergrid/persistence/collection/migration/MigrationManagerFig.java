package org.apache.usergrid.persistence.collection.migration;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Configuration for the MigrationManager.
 */
@FigSingleton
public interface MigrationManagerFig extends GuicyFig {
    @Key( "collections.keyspace.strategy.class" )
    @Default( "org.apache.cassandra.locator.SimpleStrategy" )
    String getStrategyClass();

    @Key( "collections.keyspace.strategy.options" )
    String getStrategyOptions();

}
