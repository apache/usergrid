package org.apache.usergrid.persistence.collection.serialization;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Serialization related configuration options.
 */
@FigSingleton
public interface SerializationFig extends GuicyFig {
    @Key( "collection.stage.transient.timeout" )
    @Default( "60" )
    int getTimeout();
}
