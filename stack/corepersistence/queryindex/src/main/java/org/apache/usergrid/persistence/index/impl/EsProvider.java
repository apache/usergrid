/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.index.impl;


import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.RandomStringUtils;

import org.apache.usergrid.persistence.index.IndexFig;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Provides access to ElasticSearch client and, optionally, embedded ElasticSearch for testing.
 */
@Singleton
public class EsProvider {

    private static final Logger log = LoggerFactory.getLogger( EsProvider.class );

    private final IndexFig indexFig;
    private static Client client;
    private static Node node;

    public static String LOCAL_ES_PORT_PROPNAME = "EMBEDDED_ES_PORT";


    @Inject
    public EsProvider( IndexFig fig ) {
        this.indexFig = fig;
    }


    /**
     * Get the client instnace
     */
    public Client getClient() {
        if ( client == null ) {
            //synchronize on creating the client so we don't create too many
            createClient( indexFig );
        }
        return client;
    }


    /**
     * Reset the client instnace
     */
    public void releaseClient() {
        //reset our static variables
        if ( client != null && node != null ) {
            node.stop();
            node = null;
            client = null;
        }
    }


    private synchronized void createClient( IndexFig fig ) {


        if ( client != null && node != null) {
            return;
        }


        String allHosts = "";


            // we will connect to ES on all configured hosts
            String SEP = "";
            for ( String host : fig.getHosts().split( "," ) ) {
                allHosts = allHosts + SEP + host + ":" + fig.getPort();
                SEP = ",";
            }

        String nodeName = fig.getNodeName();
        if ( "default".equals( nodeName ) ) {
            // no nodeName was specified, use hostname
            try {
                nodeName = InetAddress.getLocalHost().getHostName();
            }
            catch ( UnknownHostException ex ) {
                nodeName = "client-" + RandomStringUtils.randomAlphabetic( 8 );
                log.warn( "Couldn't get hostname to use as ES node name, using " + nodeName );
            }
        }

        Settings settings = ImmutableSettings.settingsBuilder()

                .put( "cluster.name", fig.getClusterName() )

                        // this assumes that we're using zen for host discovery.  Putting an
                        // explicit set of bootstrap hosts ensures we connect to a valid cluster.
                .put( "discovery.zen.ping.unicast.hosts", allHosts )
                .put( "discovery.zen.ping.multicast.enabled", "false" ).put( "http.enabled", false )

                .put( "client.transport.ping_timeout", 2000 ) // milliseconds
                .put( "client.transport.nodes_sampler_interval", 100 ).put( "network.tcp.blocking", true )
                .put( "node.client", true ).put( "node.name", nodeName )

                .build();

        log.debug( "Creating ElasticSearch client with settings: " + settings.getAsMap() );

        // use this client when connecting via socket only,
        // such as ssh tunnel or other firewall issues
        // newClient  = new TransportClient(settings).addTransportAddress(
        //                  new InetSocketTransportAddress("localhost", 9300) );

        //use this client for quick connectivity
        node = NodeBuilder.nodeBuilder().settings( settings ).client( true ).node();
        client = node.client();
    }



}
