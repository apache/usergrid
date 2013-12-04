package org.apache.usergrid.perftest.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result interface from operations against runner API.
 */
public interface Result {

    /**
     * True for success false otherwise.
     *
     * @return true if success, false otherwise
     */
    @JsonProperty
    boolean getStatus();

    /**
     * Optional message response.
     *
     * @return a message if required, otherwise null
     */
    @JsonProperty
    String getMessage();

    @JsonProperty
    String getEndpoint();
}
