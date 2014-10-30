package org.apache.usergrid.persistence.queue;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

@FigSingleton
public interface QueueFig extends GuicyFig {

    @Key( "usergrid.queue.region" )
    @Default("us-east-1")
    public String getRegion();

    @Key( "usergrid.queue.prefix" )
    @Default("usergrid")
    public String getPrefix();

}
