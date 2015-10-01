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


import org.apache.usergrid.chop.api.SshValues;
import org.apache.usergrid.chop.stack.Instance;
import com.google.common.base.Preconditions;


/**
 * A simple values holder for Amazon Instance based associations.
 */
public class InstanceValues implements SshValues {

    private String sshKeyFile;
    private Instance instance;


    public InstanceValues( Instance instance, String sshKeyFile ) {
        Preconditions.checkNotNull( sshKeyFile, "The 'sshKeyFile' parameter cannot be null." );
        Preconditions.checkNotNull( instance, "The 'instance parameter cannot be null." );
        Preconditions.checkState( instance.getPublicIpAddress() != null && ( ! instance.getPublicIpAddress().isEmpty() )
                                , "Public IP address field of the 'instance' parameter cannot be empty" );

        this.sshKeyFile = sshKeyFile;
        this.instance = instance;
    }


    @Override
    public String getHostname() {
        return instance.getPublicDnsName();
    }


    @Override
    public String getPublicIpAddress() {
        return instance.getPublicIpAddress();
    }


    @Override
    public String getSshKeyFile() {
        return sshKeyFile;
    }
}

