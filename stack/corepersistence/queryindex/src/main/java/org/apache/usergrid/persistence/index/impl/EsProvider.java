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

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.util.AvailablePortFinder;
import org.apache.usergrid.persistence.index.IndexFig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;

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

                int port = AvailablePortFinder.getNextAvailable(2000);

                File tempDir;
                try {
                    tempDir = getTempDirectory();
                } catch (Exception ex) {
                    throw new RuntimeException(
                        "Fatal error unable to create temp dir, start embedded ElasticSearch", ex);
                }

                Settings settings = ImmutableSettings.settingsBuilder()
                    .put("node.http.enabled", true)
                    .put("transport.tcp.port", port)
                    .put("path.logs", tempDir.toString())
                    .put("path.data", tempDir.toString())
                    .put("gateway.type", "none").put("index.store.type", "memory")
                    .put("index.number_of_shards", 1)
                    .put("index.number_of_replicas", 1).build();

                log.info("Starting ElasticSearch embedded with settings: " + settings.getAsMap());

                Node node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
                newClient = node.client();

            } else { // build client that connects to all hosts

                final String hosts = fig.getHosts();

                Settings settings = ImmutableSettings.settingsBuilder()
                    .put("client.transport.ping_timeout", 2000) // milliseconds
                    .put("client.transport.nodes_sampler_interval", 100).put("http.enabled", false)

                    // this assumes that we're using zen for host discovery.  Putting an 
                    // explicit set of bootstrap hosts ensures we connect to a valid cluster.
                    .put("discovery.zen.ping.unicast.hosts", hosts).build();

                Node node = NodeBuilder.nodeBuilder().settings(settings)
                    .clusterName(fig.getClusterName())
                    .client(true).node();

                newClient = node.client();

            }
            client = newClient;
        }
        return client;
    }


    /**
     * Uses a project.properties file that Maven does substitution on to to replace the value of a 
     * property with the path to the Maven build directory (a.k.a. target). It then uses this path 
     * to generate a random String which it uses to append a path component to so a unique directory 
     * is selected. If already present it's deleted, then the directory is created.
     *
     * @return a unique temporary directory
     *
     * @throws IOException if we cannot access the properties file
     */
    public static File getTempDirectory() throws IOException {
        File tmpdir;
        Properties props = new Properties();
        props.load( ClassLoader.getSystemResourceAsStream( "project.properties" ) );
        File basedir = new File( ( String ) props.get( "target.directory" ) );
        String comp = RandomStringUtils.randomAlphanumeric( 7 );
        tmpdir = new File( basedir, comp );

        if ( tmpdir.exists() ) {
            log.info( "Deleting directory: {}", tmpdir );
            FileUtils.forceDelete( tmpdir );
        }
        else {
            log.info( "Creating temporary directory: {}", tmpdir );
            FileUtils.forceMkdir( tmpdir );
        }

        return tmpdir;
    }
}
