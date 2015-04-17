package org.apache.usergrid.persistence.queue;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

@FigSingleton
public interface QueueFig extends GuicyFig {

    @Key( "usergrid.queue.region" )
    @Default("us-east-1")
    String getRegion();

    @Key( "usergrid.queue.prefix" )
    @Default("usergrid")
    String getPrefix();

    @Key("usergrid.queue.arn")
    @Default("arn:aws:sqs:us-east-1:123456789012")
    String getArn();

    @Key( "usergrid.queue.max.receive" )
    @Default("5")
    long getMaxReceiveCount();
}
