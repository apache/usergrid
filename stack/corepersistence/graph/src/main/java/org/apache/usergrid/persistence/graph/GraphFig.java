package org.apache.usergrid.persistence.graph;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 *
 *
 */
public interface GraphFig extends GuicyFig {

    @Default( "1000" )
    @Key( "usergrid.graph.scan.page.size" )
    int getScanPageSize();
}

