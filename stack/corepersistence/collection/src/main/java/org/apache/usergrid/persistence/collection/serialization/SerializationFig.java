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

    String COLLECTION_MAX_ENTITY_SIZE = "collection.max.entity.size";

    /**
     * Time to live timeout in seconds.
     *
     * @return Timeout in seconds.
     */
    @Key("collection.stage.transient.timeout")
    @Default("5")
    int getTimeout();


    /**
     * Number of items to buffer.
     *
     * @return Number of items to buffer in memory
     */
    @Key("buffer.size")
    @Default("100")
    int getBufferSize();


    /**
     * The maximum amount of entities we can load in a single request
     * TODO, change this and move it into a common setting that both query and collection share
     */
    @Key( "collection.max.load.size" )
    @Default( "1000" )
    int getMaxLoadSize();


    /**
     * The maximum number of bytes a serialized entity can be.  Any thing beyond this is rejected
     * This default is based on the following equation
     *
     * (15mb thrift buffer * .9) / 100 (default max load size)
     */
    @Key( COLLECTION_MAX_ENTITY_SIZE )
    @Default( "5000000" )
    int getMaxEntitySize();

}
