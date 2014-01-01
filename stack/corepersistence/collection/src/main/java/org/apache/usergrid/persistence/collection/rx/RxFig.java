package org.apache.usergrid.persistence.collection.rx;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Configuration interface for RxJava classes.
 */
@FigSingleton
public interface RxFig extends GuicyFig {

    /**
     * Max number of threads a pool can allocate.  Can be dynamically changed after starting
     */
    @Key( "rx.cassandra.io.threads" )
    @Default( "20" )
    int getMaxThreadCount();
}
