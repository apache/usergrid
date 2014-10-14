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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.usergrid.chop.api.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Ec2Metadata implementation for the unit testing environment.
 */
public class Ec2Metadata {
    private static final String PUBLIC_HOSTNAME_KEY = "public-hostname";
    private static final String PUBLIC_IPV4_KEY = "public-ipv4";

    private static final Logger LOG = LoggerFactory.getLogger( Ec2Metadata.class );

    private static final boolean RUNNING_IN_EC2;
    private static String PUBLIC_HOSTNAME;
    private static String PUBLIC_IPV4_ADDRESS;
    private static final String EC2METADATA_PROCESS = "/usr/bin/ec2metadata";


    static {
        if ( new File( EC2METADATA_PROCESS ).exists() ) {
            RUNNING_IN_EC2 = true;

            try {
                File file = File.createTempFile( "ec2metadata", "out" );
                ProcessBuilder pb = new ProcessBuilder( EC2METADATA_PROCESS );
                pb.redirectOutput( file );
                Process process = pb.start();

                try {
                    process.waitFor();
                }
                catch ( InterruptedException e ) {
                    LOG.error( "Interrupted while waiting for process {}", EC2METADATA_PROCESS, e );
                }

                Properties props = new Properties();
                props.load( new FileInputStream( file ) );
                PUBLIC_HOSTNAME = props.getProperty( PUBLIC_HOSTNAME_KEY );
                PUBLIC_IPV4_ADDRESS = props.getProperty( PUBLIC_IPV4_KEY );
            }
            catch ( IOException e ) {
                LOG.error( "Failed to execute process {}", EC2METADATA_PROCESS, e );
                PUBLIC_IPV4_ADDRESS = "127.0.0.1";
                PUBLIC_HOSTNAME = "localhost";
            }
        }
        else {
            RUNNING_IN_EC2 = false;
            PUBLIC_HOSTNAME = getHostnameNormal();
            PUBLIC_IPV4_ADDRESS = getIpv4AddressNormal();
        }
    }


    public static void applyBypass( Runner runner ) {
        runner.bypass( Runner.HOSTNAME_KEY, PUBLIC_HOSTNAME );
        runner.bypass( Runner.IPV4_KEY, PUBLIC_IPV4_ADDRESS );
    }


    public static String getHostname() {
        return PUBLIC_HOSTNAME;
    }


    public static String getIpv4Address() {
        return PUBLIC_IPV4_ADDRESS;
    }


    public static boolean isRunningInEc2() {
        return RUNNING_IN_EC2;
    }


    private static String getHostnameNormal() {
        try {
            InetAddress addr = InetAddress.getLocalHost();

            // Get hostname
            return addr.getHostName();
        }
        catch ( UnknownHostException e )  {
            throw new RuntimeException( "Failed to acquire the hostname." );
        }
    }


    private static String getIpv4AddressNormal() {
        try {
            return InetAddress.getByName( getHostnameNormal() ).getHostAddress();
        }
        catch ( UnknownHostException e ) {
            throw new RuntimeException( "Failed to acquire the address of host" );
        }
    }
}
