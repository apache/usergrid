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

    /**
     * Time to live timeout in seconds.
     *
     * @return Timeout in seconds.
     */
    @Key("collection.stage.transient.timeout")
    @Default("5")
    int getTimeout();

    /**
     * Number of history items to return for delete.
     *
     * @return Timeout in seconds.
     */
    @Key("collection.delete.history.size")
    @Default("100")
    int getHistorySize();

    /**
     * Number of items to buffer.
     *
     * @return Timeout in seconds.
     */
    @Key("collection.buffer.size")
    @Default("10")
    int getBufferSize();


    /**
     * The size of threads to have in the task pool
     */
    @Key( "collection.task.pool.threadsize" )
    @Default( "20" )
    int getTaskPoolThreadSize();



    /**
     * The size of threads to have in the task pool
     */
    @Key( "collection.task.pool.queuesize" )
    @Default( "20" )
    int getTaskPoolQueueSize();

    /**
     * The maximum amount of entites we can load at any one time
     * @return
     */
    @Key( "collection.max.load.size" )
    @Default( "1000" )
    int getMaxLoadSize();



    /**
     * The maximum number of bytes a serialized entity can be.  Any thing beyond this is rejected
     * @return
     */
    @Key( "collection.max.entry.size" )
    @Default( "512" )
    int getMaxEntrySize();

}
