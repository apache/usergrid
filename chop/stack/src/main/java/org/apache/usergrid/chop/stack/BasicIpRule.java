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
import java.util.Collections;
import java.util.List;


/**
 * A basic IpRule implementation.
 */
public class BasicIpRule implements IpRule {
    private String ipProtocol;
    private Integer toPort;
    private Integer fromPort;
    private List<String> ipRanges = new ArrayList<String>();


    @Override
    public String getIpProtocol() {
        return ipProtocol;
    }


    public void setIpProtocol( final String ipProtocol ) {
        this.ipProtocol = ipProtocol;
    }


    @Override
    public IpRule withIpProtocol( final String ipProtocol ) {
        this.ipProtocol = ipProtocol;
        return this;
    }


    @Override
    public Integer getFromPort() {
        return fromPort;
    }


    public void setFromPort( Integer fromPort ) {
        this.fromPort = fromPort;
    }


    @Override
    public IpRule withFromPort( final Integer fromPort ) {
        this.fromPort = fromPort;
        return this;
    }


    @Override
    public Integer getToPort() {
        return toPort;
    }


    @Override
    public IpRule withToPort( final Integer toPort ) {
        this.toPort = toPort;
        return this;
    }


    public void setToPort( Integer toPort ) {
        this.toPort = toPort;
    }


    @Override
    public List<String> getIpRanges() {
        return ipRanges;
    }


    public void setIpRanges( List<String> ipRanges ) {
        this.ipRanges = ipRanges;
    }


    @Override
    public IpRule withIpRanges( final String... ipRanges ) {
        Collections.addAll( this.ipRanges, ipRanges );
        return this;
    }


    @Override
    public boolean equals( final Object obj ) {
        if( ! ( obj instanceof IpRule ) ) {
            return false;
        }
        IpRule rule = ( IpRule )obj;

        if( ! fromPort.equals( rule.getFromPort() ) || ! toPort.equals( rule.getToPort() ) ||
                ! ipProtocol.equals( rule.getIpProtocol() ) || ipRanges.size() != rule.getIpRanges().size() ) {
            return false;
        }

        for( String myRange: ipRanges ) {
            boolean exists = false;
            for( String range: rule.getIpRanges() ) {
                if( myRange.equals( range ) ) {
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
