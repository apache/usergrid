/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.usergrid.chop.webapp.elasticsearch;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.annotations.SerializedName;


public class ElasticSearchNode {

    final String TRANSPORT_ADDRESS = "transport_address";
    final String HTTP_ADDRESS = "http_address";

    private String name;
    private String host;
    private String ip;

    @SerializedName( TRANSPORT_ADDRESS )
    private String transportAddress;

    @SerializedName( HTTP_ADDRESS )
    private String httpAddress;


    /**
     * Parse the result returned from ElasticSearch which seems like "inet[/10.0.0.1:9300]"
     * @return host ip address
     */
    public String getTransportHost() {
        int host_begin;
        int host_end;
        host_begin = transportAddress.indexOf( "/" )+1;
        host_end = transportAddress.indexOf( ":" );
        return transportAddress.substring( host_begin, host_end );
    }


    /**
     * Parse the result returned from ElasticSearch which seems like "inet[/10.0.0.1:9300]"
     * @return host port number
     */
    public int getTransportPort() {
        int port_begin;
        int port_end;
        port_begin = transportAddress.indexOf( ":" ) + 1;
        port_end = transportAddress.indexOf( "]" );
        return Integer.parseInt( transportAddress.substring( port_begin, port_end ));
    }


    /**
     * Parse the result returned from ElasticSearch which seems like "inet[/10.0.0.1:9300]"
     * @return host port number
     */
    public int getHTTPPort() {
        int port_begin;
        int port_end;
        port_begin = httpAddress.indexOf( ":" ) + 1;
        port_end = httpAddress.indexOf( "]" );
        return Integer.parseInt( httpAddress.substring( port_begin, port_end ));
    }


    public String getName() {
        return name;
    }


    public void setName( String name ) {
        this.name = name;
    }


    public String getTransportAddress() {
        return transportAddress;
    }


    public String getHttpAddress() {
        return httpAddress;
    }


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString( this );
        } catch ( JsonProcessingException e ) {
            e.printStackTrace();
            return "Failed serialization";
        }
    }
}