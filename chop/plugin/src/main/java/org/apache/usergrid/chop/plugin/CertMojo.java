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
package org.apache.usergrid.chop.plugin;


import java.net.URI;

import org.apache.usergrid.chop.api.ChopUtils;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;


/** @todo is this still necessary? */
@Mojo( name = "cert" )
public class CertMojo extends MainMojo {

    @Override
    public void execute() throws MojoExecutionException {
        try {
            ChopUtils.installRunnerKey( null, "tomcat" );

            URI uri = URI.create( endpoint );
            if ( certStorePassphrase == null ) {
                ChopUtils.installCert( uri.getHost(), uri.getPort(), null );
            }
            else {
                ChopUtils.installCert( uri.getHost(), uri.getPort(), certStorePassphrase.toCharArray() );
            }
        }
        catch ( Exception e ) {
            getLog().error( "Failed to install certificate!", e );
        }
    }
}