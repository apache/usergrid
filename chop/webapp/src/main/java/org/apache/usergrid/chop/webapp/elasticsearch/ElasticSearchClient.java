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
package org.apache.usergrid.chop.webapp.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;


@Singleton
public class ElasticSearchClient implements IElasticSearchClient {

    private Client client;
    private String host;
    private int port;
    private String clusterName;


    @Inject
    public ElasticSearchClient(ElasticSearchFig elasticFig) {
        Settings settings = ImmutableSettings.settingsBuilder().build();

        client = new TransportClient(settings).addTransportAddress(
                new InetSocketTransportAddress(elasticFig.getTransportHost(), elasticFig.getTransportPort()));
        port = elasticFig.getTransportPort();
        host = elasticFig.getTransportHost();
        clusterName = elasticFig.getClusterName();
    }


    @Override
    public Client getClient() {
        return client;
    }


    @Override
    public String getHost() {
        return host;
    }


    @Override
    public int getPort() {
        return port;
    }


    @Override
    public String getClusterName() {
        return clusterName;
    }


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Failed serialization";
        }
    }
}
