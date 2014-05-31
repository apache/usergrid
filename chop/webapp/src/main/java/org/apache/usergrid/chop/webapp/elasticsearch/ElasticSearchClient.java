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
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Singleton
public class ElasticSearchClient implements IElasticSearchClient {

    private final static Logger LOG = LoggerFactory.getLogger(ElasticSearchClient.class);

    private Client client;
    private String host;
    private int transportPort;
    private int httpPort;
    private String clusterName;
    private List<ElasticSearchNode> nodeList;

    @Inject
    private ElasticSearchFig elasticSearchFig;


    @Override
    public Client start() {
        transportPort = elasticSearchFig.getTransportPort();
        httpPort = elasticSearchFig.getHttpPort();
        host = elasticSearchFig.getTransportHost();
        clusterName = elasticSearchFig.getClusterName();

        Settings settings = ImmutableSettings.settingsBuilder().put( "cluster.name", clusterName ).build();
        LOG.info( "Connecting Elasticsearch on {}", elasticSearchFig.getTransportHost() + ":" +
                elasticSearchFig.getTransportPort() );
        nodeList = getNodeList();
        TransportClient transportClient = new TransportClient( settings );
        for ( ElasticSearchNode elasticSearchNode : nodeList ) {
            LOG.debug( "Adding transport address with host {} and port {}", elasticSearchNode.getTransportHost()
                    , elasticSearchNode.getTransportPort() );
            transportClient.addTransportAddress( new InetSocketTransportAddress( elasticSearchNode.getTransportHost(),
                    elasticSearchNode.getTransportPort() ) );
        }

        client = transportClient;
        return client;
    }


    @Override
    public List<ElasticSearchNode> getNodeList() {
        List<ElasticSearchNode> nodeList = new ArrayList<ElasticSearchNode>();
        String result = getHTTPResult( "/_nodes" );
        Gson gson = new Gson();
        ElasticSearchNodeResponse response = gson.fromJson( result, ElasticSearchNodeResponse.class );
        Map<String, ElasticSearchNode> nodes = response.getNodes();

        for ( Map.Entry<String, ElasticSearchNode> entry : nodes.entrySet() ) {
            LOG.debug( "Adding node {}",entry.getValue() );
            nodeList.add( entry.getValue() );
        }

        setNodeList( nodeList );
        return nodeList;
    }


    @Override
    public String getHTTPResult( String query ) {
        URL url = null;
        try {
            url = new URL( "http://" + getHost() + ":" + getHttpPort() + query );
        } catch ( MalformedURLException e ) {
            e.printStackTrace();
            LOG.error( "Failed to create url {}", url , e );
        }
        if (url != null) {
            try {
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod( "GET" );
                int responseCode = con.getResponseCode();
                LOG.debug( "Response Code : " + responseCode );

                BufferedReader in = new BufferedReader( new InputStreamReader( con.getInputStream() ) );
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ( ( inputLine = in.readLine() ) != null ) {
                    response.append( inputLine );
                }
                in.close();
                return response.toString();
            } catch ( IOException e ) {
                e.printStackTrace();
                LOG.error( "Failed to get the result for {}", url , e );
            }
        }
        return null;
    }


    public void setNodeList( List<ElasticSearchNode> nodeList ) {
        this.nodeList = nodeList;
    }


    public int getHttpPort() {
        return httpPort;
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
    public int getTransportPort() {
        return transportPort;
    }


    @Override
    public String getClusterName() {
        return clusterName;
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
