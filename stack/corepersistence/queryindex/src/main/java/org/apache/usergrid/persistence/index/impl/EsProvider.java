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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.core.util.AvailablePortFinder;
import org.apache.usergrid.persistence.index.IndexFig;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to ElasticSearch client.
 */
@Singleton
public class EsProvider {

    private static final Logger log = LoggerFactory.getLogger(EsProvider.class);

    private final IndexFig indexFig;
    private static Client client;

    @Inject
    public EsProvider(IndexFig fig) {
        this.indexFig = fig;
    }

    public synchronized Client getClient() {
        if (client == null) {
            client = getClient(indexFig);
        }
        return client;
    }

    public static synchronized Client getClient(IndexFig fig) {

        if (client == null) {

            Client newClient = null;

            if (fig.isEmbedded()) {

                int port = AvailablePortFinder.getNextAvailable( 2000 );

                Settings settings = ImmutableSettings.settingsBuilder()
                        .put("node.http.enabled", true)
                        .put("transport.tcp.port", port)
                        .put("path.logs", "target/elasticsearch/logs_" + port)
                        .put("path.data", "target/elasticsearch/data_" + port)
                        .put("gateway.type", "none")
                        .put("index.store.type", "memory")
                        .put("index.number_of_shards", 1)
                        .put("index.number_of_replicas", 1)
                        .build();

                log.info("Starting ElasticSearch embedded with settings: " +  settings.getAsMap());

                Node node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
                newClient = node.client();

            } else { // build client that connects to all hosts

                Settings settings = ImmutableSettings.settingsBuilder()
                        .put("cluster.name", fig.getClusterName() )
                        // TODO: consider making these configurable
                        .put("client.transport.ignore_cluster_name", true )
                        .put("client.transport.ping_timeout", 2000) // milliseconds
                        .put("client.transport.nodes_sampler_interval", 100 )
                        .build();

                log.info("Creating ElasticSearch client with settings: " +  settings.getAsMap());

                TransportClient transportClient = new TransportClient(settings);

                for (String host : fig.getHosts().split(",")) {
                    transportClient.addTransportAddress(
                            new InetSocketTransportAddress(host.trim(), fig.getPort()));
                    log.info("   Added transport for ElasticSearch host {}:{}", host.trim(), fig.getPort() ) ;
                }
                newClient = transportClient;
            }
            client = newClient;
        }
        return client;
    }
}
