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
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
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
            createClient();
        }
        return client;
    }


    /**
     * Reset the client instance
     */
    public synchronized void releaseClient() {
        //reset our static variables
        if ( client != null ) {
            try {
                client.close();
            }
            //if we fail for any reason, null it so the next request creates a new client
            finally {
                client = null;
            }
        }
    }


    /**
     * Create our client
     */
    private synchronized void createClient() {

        if ( client != null ) {
            return;
        }


        final ClientType clientType = ClientType.valueOf( indexFig.getClientType() );

        switch ( clientType ) {
            case NODE:
                client = createNodeClient();
                break;

            case TRANSPORT:
                client = createTransportClient();
                break;
            default:
                throw new RuntimeException( "Only client types of NODE and TRANSPORT are supported" );
        }
    }


    /**
     * Create the transport client
     * @return
     */
    private Client createTransportClient() {
        final String clusterName = indexFig.getClusterName();
        final int port = indexFig.getPort();

        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder().put( "cluster.name", clusterName )
                                                              .put( "client.transport.sniff", true );

        String nodeName = indexFig.getNodeName();

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

        settings.put( "node.name", nodeName );


        TransportClient transportClient = new TransportClient( settings.build() );

        // we will connect to ES on all configured hosts
        for ( String host : indexFig.getHosts().split( "," ) ) {
            transportClient.addTransportAddress( new InetSocketTransportAddress( host, port ) );
        }

        return transportClient;
    }


    /**
     * Create a node client
     * @return
     */
    public Client createNodeClient() {

        // we will connect to ES on all configured hosts


        final String clusterName = indexFig.getClusterName();
        final String nodeName = indexFig.getNodeName();
        final int port = indexFig.getPort();

        /**
         * Create our hosts
         */
        final StringBuffer hosts = new StringBuffer();

        for ( String host : indexFig.getHosts().split( "," ) ) {
            hosts.append( host ).append( ":" ).append( port ).append( "," );
        }

        //remove the last comma
        hosts.deleteCharAt( hosts.length() - 1 );

        final String hostString = hosts.toString();


        Settings settings = ImmutableSettings.settingsBuilder()

                .put( "cluster.name", clusterName )

                        // this assumes that we're using zen for host discovery.  Putting an
                        // explicit set of bootstrap hosts ensures we connect to a valid cluster.
                .put( "discovery.zen.ping.unicast.hosts", hostString )
                .put( "discovery.zen.ping.multicast.enabled", "false" ).put( "http.enabled", false )

                .put( "client.transport.ping_timeout", 2000 ) // milliseconds
                .put( "client.transport.nodes_sampler_interval", 100 ).put( "network.tcp.blocking", true )
                .put( "node.client", true ).put( "node.name", nodeName )

                .build();

        log.debug( "Creating ElasticSearch client with settings: {}",  settings.getAsMap() );

        Node node = NodeBuilder.nodeBuilder().settings( settings ).client( true ).data( false ).node();

        return node.client();
    }


    /**
     *
     */
    public enum ClientType {
        TRANSPORT,
        NODE
    }
}
