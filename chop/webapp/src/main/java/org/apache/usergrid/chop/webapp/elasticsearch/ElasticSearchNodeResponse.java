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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

import org.safehaus.guicyfig.Key;


public class ElasticSearchNodeResponse {

    final String CLUSTER_NAME = "cluster_name";

    @SerializedName( CLUSTER_NAME )
    private String clusterName;
    private Map<String, ElasticSearchNode> nodes;


    public Map<String, ElasticSearchNode> getNodes() {
        return nodes;
    }


    public void setNodes(Map<String, ElasticSearchNode> nodes) {
        this.nodes = nodes;
    }


    public String getClusterName() {

        return clusterName;
    }


    public void setClusterName( String cluster_name ) {
        this.clusterName = cluster_name;
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

