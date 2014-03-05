/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.usergrid.persistence.index.utils;

import org.apache.usergrid.persistence.collection.util.AvailablePortFinder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ElasticSearchRule extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger( ElasticSearchRule.class ); 

    private Client client; 

    @Override
    protected void after() {
        client.close();
    }

    @Override
    protected void before() throws Throwable {

        // use unique port and directory names so multiple instances of ES can run concurrently.
        int port = AvailablePortFinder.getNextAvailable(2000);
        Settings settings = ImmutableSettings.settingsBuilder()
            .put("node.http.enabled", true)
            .put("transport.tcp.port", port )
            .put("path.logs","target/elasticsearch/logs_" + port )
            .put("path.data","target/elasticsearch/data_" + port )
            .put("gateway.type", "none" )
            .put("index.store.type", "memory" )
            .put("index.number_of_shards", 1 )
            .put("index.number_of_replicas", 1).build();

        Node node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
        client = node.client();
    }

    public Client getClient() {
        return client;
    }
    
}
