package org.apache.usergrid.persistence.queue;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

@FigSingleton
public interface QueueFig extends GuicyFig {

    /**
     * Any region value string must exactly match the region names specified on this page:
     *
     * http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html*
     */


    /**
     * Primary region to use for Amazon queues.
     */
    @Key( "usergrid.queue.region" )
    @Default("us-east-1")
    public String getRegion();

    /**
     * Flag to determine if Usergrid should use a multi-region Amazon queue
     * implementation.
     */
    @Key( "usergrid.queue.multiregion" )
    @Default("false")
    public boolean isMultiRegion();

    /**
     * Comma-separated list of one or more Amazon regions to use if multiregion
     * is set to true.
     */
    @Key( "usergrid.queue.regionList" )
    @Default("us-east-1")
    public String getRegionList();


    @Key( "usergrid.queue.prefix" )
    @Default("usergrid")
    public String getPrefix();

    /**
     * Set the amount of time (in minutes) to retain messages in a queue.
     * 1209600 = 14 days (maximum retention period)
     */
    @Key( "usergrid.queue.retention" )
    @Default("1209600")
    public String getRetentionPeriod();

    /**
     * Set the amount of time (in minutes) to retain messages in a dead letter queue.
     * 1209600 = 14 days (maximum retention period)
     */
    @Key( "usergrid.queue.deadletter.retention" )
    @Default("1209600")
    public String getDeadletterRetentionPeriod();

    /**
     * The maximum number of messages to deliver to a dead letter queue.
     */
    @Key( "usergrid.queue.deliveryLimit" )
    @Default("5")
    public String getQueueDeliveryLimit();

    @Key("usergrid.use.default.queue")
    @Default("false")
    public boolean overrideQueueForDefault();
}
