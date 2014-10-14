/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper for starting "embedded" Tomcat as it's own process, for testing.
 */
public class ElasticSearchMain {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchMain.class);

    public static void main(String[] args) throws Exception {

        String clusterName = args[0];
        String port = args[1];

        File tempDir;
        try {
            tempDir = getTempDirectory();
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Fatal error unable to create temp dir, start embedded ElasticSearch", ex);
        }

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", clusterName)
                .put("network.publish_host", "127.0.0.1")
                .put("transport.tcp.port", port)
                .put("discovery.zen.ping.multicast.enabled", "false")
                .put("node.http.enabled", false)
                .put("path.logs", tempDir.toString())
                .put("path.data", tempDir.toString())
                .put("gateway.type", "none")
                .put("index.store.type", "memory")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
                .build();

        log.info("-----------------------------------------------------------------------");
        log.info("Starting forked ElasticSearch server with settings: \n" + settings.getAsMap());
        log.info("-----------------------------------------------------------------------");

        Node node = NodeBuilder.nodeBuilder().settings(settings)
                .clusterName(clusterName).node();

        while (true) {
            Thread.sleep(1000);
        }
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
