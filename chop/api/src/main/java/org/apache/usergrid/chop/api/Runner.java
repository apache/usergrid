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


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/** Minimal requirements for runner information. */
@FigSingleton
@JsonSerialize( using = RunnerSerializer.class )
@JsonDeserialize( using = RunnerDeserializer.class )
public interface Runner extends GuicyFig {
    String RESET_POST = "/reset";
    String START_POST = "/start";
    String STOP_POST  = "/stop";
    String STATUS_GET = "/status";
    String STATS_GET = "/stats";


    // ~~~~~~~~~~~~~~~~~~~~~ Runner Related Configuration ~~~~~~~~~~~~~~~~~~~~


    String IPV4_KEY = "public-ipv4";

    /**
     * Gets the IPv4 public address used by the Runner. Uses {@link Runner#IPV4_KEY}
     * to access the IPv4 public address.
     *
     * @return the IPv4 public address (octet) as a String
     */
    @JsonProperty
    @Key( IPV4_KEY )
    String getIpv4Address();


    String HOSTNAME_KEY = "public-hostname";

    /**
     * Gets the public hostname of the Runner. Uses {@link Runner#HOSTNAME_KEY} to
     * access the public hostname.
     *
     * @return the public hostname
     */
    @JsonProperty
    @Key( HOSTNAME_KEY )
    String getHostname();

    String SERVER_PORT_KEY = "server.port";
    String DEFAULT_SERVER_PORT = "8443";

    /**
     * Gets the Runner server port. Uses {@link Runner#SERVER_PORT_KEY} to access
     * the server port. The default port used is setup via {@link
     * Runner#DEFAULT_SERVER_PORT}.
     *
     * @return the Runner's server port
     */
    @JsonProperty
    @Key( SERVER_PORT_KEY )
    @Default( DEFAULT_SERVER_PORT )
    int getServerPort();


    String URL_KEY = "url.key";

    /**
     * Gets the URL of the Runner's REST interface. Uses {@link Runner#URL_KEY} to
     * access the Runner's URL.
     *
     * @return the URL of the Runner's REST interface
     */
    @JsonProperty
    @Key( URL_KEY )
    String getUrl();


    String DEFAULT_RUNNER_TEMP_DIR = "/tmp";
    String RUNNER_TEMP_DIR_KEY = "runner.temp.dir";

    /**
     * Gets the temporary directory used by the Runner to store files. Uses {@link
     * Runner#RUNNER_TEMP_DIR_KEY} to access the temp dir used by the Runner.
     *
     * @return the temporary directory used by the Runner
     */
    @JsonProperty
    @Key( RUNNER_TEMP_DIR_KEY )
    @Default( DEFAULT_RUNNER_TEMP_DIR )
    String getTempDir();
}
