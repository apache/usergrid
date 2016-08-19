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
    String getPrimaryRegion();

    /**
     * Flag to determine if Usergrid should use a multi-region Amazon queue
     * implementation.
     */
    @Key( "usergrid.queue.multiregion" )
    @Default("false")
    boolean isMultiRegion();

    /**
     * Comma-separated list of one or more Amazon regions to use if multiregion
     * is set to true.
     */
    @Key( "usergrid.queue.regionList" )
    @Default("us-east-1")
    String getRegionList();


    /**
     * Set the amount of time (in minutes) to retain messages in a queue.
     * 1209600 = 14 days (maximum retention period)
     */
    @Key( "usergrid.queue.retention" )
    @Default("1209600")
    String getRetentionPeriod();

    /**
     * Set the amount of time (in minutes) to retain messages in a dead letter queue.
     * 1209600 = 14 days (maximum retention period)
     */
    @Key( "usergrid.queue.deadletter.retention" )
    @Default("1209600")
    String getDeadletterRetentionPeriod();

    /**
     * The maximum number of attempts to attempt to deliver before failing into the DLQ
     */
    @Key( "usergrid.queue.deliveryLimit" )
    @Default("10")
    String getQueueDeliveryLimit();

    @Key("usergrid.use.default.queue")
    @Default("false")
    boolean overrideQueueForDefault();

    @Key("usergrid.queue.publish.threads")
    @Default("100")
    int getAsyncMaxThreads();

    // current msg size 1.2kb * 850000 = 1.02 GB (let this default be the most we'll queue in heap)
    @Key("usergrid.queue.publish.queuesize")
    @Default("250000")
    int getAsyncQueueSize();

    /**
     * Set the visibility timeout (in milliseconds) for faster retries
     * @return
     */
    @Key( "usergrid.queue.visibilityTimeout" )
    @Default("5000") // 5 seconds
    int getVisibilityTimeout();

    @Key( "usergrid.queue.localquorum.timeout")
    @Default("30000") // 30 seconds
    int getLocalQuorumTimeout();

    @Key( "usergrid.queue.client.connection.timeout")
    @Default( "5000" ) // 5 seconds
    int getQueueClientConnectionTimeout();

    @Key( "usergrid.queue.client.socket.timeout")
    @Default( "20000" ) // 20 seconds
    int getQueueClientSocketTimeout();

    @Key( "usergrid.queue.poll.timeshift")
    @Default( "3000" ) // 3 seconds
    int getQueuePollTimeshift();

    @Key( "usergrid.queue.quorum.fallback")
    @Default("false") // 30 seconds
    boolean getQuorumFallback();

}
