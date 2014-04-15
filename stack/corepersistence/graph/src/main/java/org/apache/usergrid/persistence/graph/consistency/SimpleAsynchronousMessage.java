package org.apache.usergrid.persistence.graph.consistency;


import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 *  Simple message that just contains the event and the timeout.  More advanced queue implementations
 *  will most likely subclass this class.
 *
 */
public class SimpleAsynchronousMessage<T> implements AsynchronousMessage<T>, Serializable {

    @JsonTypeInfo( use= JsonTypeInfo.Id.MINIMAL_CLASS,include= JsonTypeInfo.As.WRAPPER_OBJECT,property="@class" )
    @JsonProperty
    private final T event;
    @JsonProperty
    private final long timeout;


    @JsonCreator
    public SimpleAsynchronousMessage(@JsonProperty("event") final T event, @JsonProperty("timeout") final long timeout ) {
        this.event = event;
        this.timeout = timeout;
    }


    @Override
    public T getEvent() {
       return event;
    }


    @Override
    public long getTimeout() {
        return timeout;
    }
}
