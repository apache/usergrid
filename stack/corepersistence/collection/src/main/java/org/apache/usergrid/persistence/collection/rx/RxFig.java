package org.apache.usergrid.persistence.collection.rx;


import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Configuration interface for RxJava classes.
 */
public interface RxFig extends GuicyFig {

    /**
     * Max number of threads a pool can allocate.  Can be dynamically changed after starting
     */
    @Key( "rx.cassandra.io.threads" )
    int getMaxThreadCount();
}
