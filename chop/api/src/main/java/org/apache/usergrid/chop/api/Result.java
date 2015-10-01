/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/** Result interface from operations against runner API. */
@JsonDeserialize( as = BaseResult.class )
public interface Result {

    /**
     * True for success false otherwise.
     *
     * @return true if success, false otherwise
     */
    @JsonProperty
    boolean getStatus();

    /**
     * Gets the present state of the runner after an operation.
     *
     * @return the current state of the runner
     */
    @JsonProperty
    State getState();

    /**
     * Optional message response.
     *
     * @return a message if required, otherwise null
     */
    @JsonProperty
    String getMessage();

    /**
     * The full URL for the end point of the operation performed.
     *
     * @return the full URL for the end point
     */
    @JsonProperty
    String getEndpoint();


    @JsonProperty
    Project getProject();
}
