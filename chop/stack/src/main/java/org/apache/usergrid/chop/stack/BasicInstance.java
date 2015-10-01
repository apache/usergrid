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


public class BasicInstance implements Instance {

    private String id;
    private InstanceSpec spec;
    private InstanceState state;
    private String privateDnsName;
    private String publicDnsName;
    private String privateIpAddress;
    private String publicIpAddress;


    private BasicInstance() {

    }


    public BasicInstance( final String id, final InstanceSpec spec, final InstanceState state,
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


    public void setId( final String id ) {
        this.id = id;
    }


    public void setSpec( final InstanceSpec spec ) {
        this.spec = spec;
    }


    public void setState( final InstanceState state ) {
        this.state = state;
    }


    public void setPrivateDnsName( final String privateDnsName ) {
        this.privateDnsName = privateDnsName;
    }


    public void setPublicDnsName( final String publicDnsName ) {
        this.publicDnsName = publicDnsName;
    }


    public void setPrivateIpAddress( final String privateIpAddress ) {
        this.privateIpAddress = privateIpAddress;
    }


    public void setPublicIpAddress( final String publicIpAddress ) {
        this.publicIpAddress = publicIpAddress;
    }


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof BasicInstance ) ) {
            return false;
        }

        final BasicInstance that = ( BasicInstance ) o;

        if ( id != null ? !id.equals( that.id ) : that.id != null ) {
            return false;
        }
        if ( privateDnsName != null ? ! privateDnsName.equals( that.privateDnsName ) : that.privateDnsName != null ) {
            return false;
        }
        if ( privateIpAddress != null ? ! privateIpAddress.equals( that.privateIpAddress ) :
             that.privateIpAddress != null ) {
            return false;
        }
        if ( publicDnsName != null ? ! publicDnsName.equals( that.publicDnsName ) : that.publicDnsName != null ) {
            return false;
        }
        if ( publicIpAddress != null ? ! publicIpAddress.equals( that.publicIpAddress ) :
             that.publicIpAddress != null ) {
            return false;
        }
        if ( spec != null ? !spec.equals( that.spec ) : that.spec != null ) {
            return false;
        }
        if ( state != that.state ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + ( spec != null ? spec.hashCode() : 0 );
        result = 31 * result + ( state != null ? state.hashCode() : 0 );
        result = 31 * result + ( privateDnsName != null ? privateDnsName.hashCode() : 0 );
        result = 31 * result + ( publicDnsName != null ? publicDnsName.hashCode() : 0 );
        result = 31 * result + ( privateIpAddress != null ? privateIpAddress.hashCode() : 0 );
        result = 31 * result + ( publicIpAddress != null ? publicIpAddress.hashCode() : 0 );
        return Math.abs( result );
    }
}
