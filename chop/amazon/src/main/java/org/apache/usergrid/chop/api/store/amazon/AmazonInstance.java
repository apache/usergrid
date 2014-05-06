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
package org.apache.usergrid.chop.api.store.amazon;


import org.apache.usergrid.chop.stack.Instance;
import org.apache.usergrid.chop.stack.InstanceState;
import org.apache.usergrid.chop.stack.InstanceSpec;


public class AmazonInstance implements Instance {

    private String id;
    private InstanceSpec spec;
    private InstanceState state;
    private String privateDnsName;
    private String publicDnsName;
    private String privateIpAddress;
    private String publicIpAddress;


    public AmazonInstance( final String id, final InstanceSpec spec, final InstanceState state,
                           final String privateDnsName, final String publicDnsName, final String privateIpAddress,
                           final String publicIpAddress ) {
        this.id = id;
        this.spec = spec;
        this.state = state;
        this.privateDnsName = privateDnsName;
        this.publicDnsName = publicDnsName;
        this.privateIpAddress = privateIpAddress;
        this.publicIpAddress = publicIpAddress;
    }


    @Override
    public String getId() {
        return id;
    }


    @Override
    public InstanceSpec getSpec() {
        return spec;
    }


    /**
     * @return Returns the last checked state of the instance
     */
    @Override
    public InstanceState getState() {
        return state;
    }


    @Override
    public String getPrivateDnsName() {
        return privateDnsName;
    }


    @Override
    public String getPublicDnsName() {
        return publicDnsName;
    }


    @Override
    public String getPrivateIpAddress() {
        return privateIpAddress;
    }


    @Override
    public String getPublicIpAddress() {
        return publicIpAddress;
    }
}
