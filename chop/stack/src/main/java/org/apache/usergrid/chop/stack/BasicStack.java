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
package org.apache.usergrid.chop.stack;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A basic Stack implementation.
 */
public class BasicStack implements Stack {
    private String name;
    private UUID id = UUID.randomUUID();
    private List<Cluster> clusters = new ArrayList<Cluster>();
    private BasicIpRuleSet ruleSet = new BasicIpRuleSet();
    private String dataCenter;


    @Override
    public String getName() {
        return name;
    }


    public BasicStack setName( final String name ) {
        this.name = name;
        return this;
    }


    @Override
    public UUID getId() {
        return id;
    }


    public BasicStack setId( UUID id ) {
        this.id = id;
        return this;
    }


    @Override
    public List<? extends Cluster> getClusters() {
        return clusters;
    }


    public BasicStack setClusters( List<Cluster> clusters ) {
        this.clusters = clusters;
        return this;
    }


    public BasicStack add( Cluster cluster ) {
        clusters.add( cluster );
        return this;
    }


    @Override
    public IpRuleSet getIpRuleSet() {
        return ruleSet;
    }


    public BasicStack setRuleSetName( String name ) {
        this.ruleSet.setName( name );
        return this;
    }


    public BasicStack setIpRuleSet( final BasicIpRuleSet ruleSet ) {
        this.ruleSet = ruleSet;
        return this;
    }


    public BasicStack addInboundRule( IpRule rule ) {
        ruleSet.getInboundRules().add( rule );
        return this;
    }


    public BasicStack addOutboundRule( IpRule rule ) {
        ruleSet.getOutboundRules().add( rule );
        return this;
    }

    @Override
    public String getDataCenter() {
        return dataCenter;
    }


    public BasicStack setDataCenter( final String dataCenter ) {
        this.dataCenter = dataCenter;
        return this;
    }

}
