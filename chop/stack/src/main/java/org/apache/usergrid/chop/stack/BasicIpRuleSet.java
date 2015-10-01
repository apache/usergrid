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


import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * A basic IpRuleSet implementation.
 */
public class BasicIpRuleSet implements IpRuleSet {
    private String name;
    private UUID id = UUID.randomUUID();
    private Set<IpRule> inboundRules = new HashSet<IpRule>();
    private Set<IpRule> outboundRules = new HashSet<IpRule>();


    @Override
    public String getName() {
        return name;
    }


    public void setName( String name ) {
        this.name = name;
    }


    @Override
    public UUID getId() {
        return id;
    }


    public void setId( UUID id ) {
        this.id = id;
    }


    @Override
    public Set<IpRule> getInboundRules() {
        return inboundRules;
    }


    public BasicIpRuleSet addInboundRule( IpRule rule ) {
        this.inboundRules.add( rule );
        return this;
    }


    @Override
    public Set<IpRule> getOutboundRules() {
        return outboundRules;
    }


    public BasicIpRuleSet addOutboundRule( IpRule rule ) {
        this.outboundRules.add( rule );
        return this;
    }


    @Override
    public boolean equals( final Object obj ) {
        if( ! ( obj instanceof IpRuleSet ) ) {
            return false;
        }
        IpRuleSet set = ( IpRuleSet )obj;

        if ( ! name.equals( set.getName() ) || inboundRules.size() != set.getInboundRules().size() ||
                outboundRules.size() != outboundRules.size() ) {
            return false;
        }

        for( IpRule myRule: inboundRules ) {
            boolean exists = false;
            for ( IpRule rule: set.getInboundRules() ) {
                if( myRule.equals( rule ) ) {
                    exists = true;
                    break;
                }
            }
            if( ! exists ) {
                return false;
            }
        }

        return true;
    }
}
