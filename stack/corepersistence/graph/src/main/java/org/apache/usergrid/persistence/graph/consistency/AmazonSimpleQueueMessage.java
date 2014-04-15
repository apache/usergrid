package org.apache.usergrid.persistence.graph.consistency;


import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 *
 *
 */
public class AmazonSimpleQueueMessage<T> implements AsynchronousMessage<T>, Serializable {

    @JsonTypeInfo( use= JsonTypeInfo.Id.CLASS,include= JsonTypeInfo.As.WRAPPER_OBJECT,property="@class" )
    @JsonProperty
    private final T event;
    @JsonProperty
    private final long timeout;
    @JsonProperty
    private final String messageId;
    @JsonProperty
    private final String receiptHandle;


    @JsonCreator
    public AmazonSimpleQueueMessage(@JsonProperty("event") final T event, @JsonProperty("timeout") final long timeout,
                                    final String messageId, final String receiptHandle) {
        this.event = event;
        this.timeout = timeout;
        this.messageId = messageId;
        this.receiptHandle = receiptHandle;
    }


    @Override
    public T getEvent() {
        return event;
    }


    @Override
    public long getTimeout() {
        return timeout;
    }


    public String getMessageId() {
        return messageId;
    }


    public String getReceiptHandle() {
        return receiptHandle;
    }
}
