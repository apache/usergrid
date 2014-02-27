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
import org.apache.usergrid.persistence.collection.util.AvailablePortFinder;
import org.apache.usergrid.persistence.index.IndexFig;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

@Singleton
public class EsProvider {

    private final IndexFig indexFig;
    private Client client;

    @Inject
    public EsProvider( IndexFig fig ) {
        this.indexFig = fig;
    }

    public synchronized Client getClient() {
        if ( client == null ) {
            if ( indexFig.isEmbedded() ) {
                // unique port and directory names so multiple instances of ES can run concurrently.
                int port = AvailablePortFinder.getNextAvailable( 2000 );
                Settings settings = ImmutableSettings.settingsBuilder()
                        .put( "node.http.enabled", true )
                        .put( "transport.tcp.port", port )
                        .put( "path.logs", "target/elasticsearch/logs_" + port )
                        .put( "path.data", "target/elasticsearch/data_" + port )
                        .put( "gateway.type", "none" )
                        .put( "index.store.type", "memory" )
                        .put( "index.number_of_shards", 1 )
                        .put( "index.number_of_replicas", 1 ).build();

                Node node = NodeBuilder.nodeBuilder().local( true ).settings( settings ).node();
                client = node.client();
            
            } else {
                throw new UnsupportedOperationException("Non-embedded ElasticSearch not yet supported");
            }
        }
        return client;
    }
}
