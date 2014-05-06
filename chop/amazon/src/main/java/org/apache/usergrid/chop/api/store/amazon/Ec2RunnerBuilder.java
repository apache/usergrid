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


import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.usergrid.chop.api.Runner;
import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.OptionState;
import org.safehaus.guicyfig.Overrides;


/**
 * Builds a Runner in the EC2 environment.
 */
public class Ec2RunnerBuilder {
    private static final String SERVLET_TEMP_DIR = "javax.servlet.context.tempdir";

    private Properties props = new Properties();

    private String ipv4Address;
    private String hostname;
    private int serverPort;
    private String url;
    private String runnerTempDir;


    /**
     * Creates a runner builder that builds a Runner using values in a
     * properties file. Used primarily to create a representation of a
     * remote runner.
     *
     * @param in the InputStream to the properties file
     * @throws IOException if there is a problem reading from the properties file input stream
     */
    Ec2RunnerBuilder( InputStream in ) throws IOException {
        props.load( in );
        extractValues();

        if ( props.containsKey( SERVLET_TEMP_DIR ) ) {
            props.setProperty( Runner.RUNNER_TEMP_DIR_KEY, props.getProperty( SERVLET_TEMP_DIR ) );
        }
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public Ec2RunnerBuilder setIpv4Address( String ipv4Address ) {
        this.ipv4Address = ipv4Address;
        return this;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public Ec2RunnerBuilder setHostname( String hostname ) {
        this.hostname = hostname;
        return this;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public Ec2RunnerBuilder setServerPort( int serverPort ) {
        this.serverPort = serverPort;
        return this;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public Ec2RunnerBuilder setUrl( String url ) {
        this.url = url;
        return this;
    }


    @SuppressWarnings( "UnusedDeclaration" )
    public Ec2RunnerBuilder setRunnerTempDir( String runnerTempDir ) {
        this.runnerTempDir = runnerTempDir;
        return this;
    }


    private void extractValues() {
        ipv4Address = props.getProperty( Runner.IPV4_KEY );
        hostname = props.getProperty( Runner.HOSTNAME_KEY );
        serverPort = Integer.parseInt( props.getProperty( Runner.SERVER_PORT_KEY, "-1" ) );
        url = props.getProperty( Runner.URL_KEY );
        runnerTempDir = props.getProperty( Runner.RUNNER_TEMP_DIR_KEY );
    }


    public Runner getRunner() {
        return new Runner() {

            @Override
            public String getIpv4Address() {
                return ipv4Address;
            }


            @Override
            public String getHostname() {
                return hostname;
            }


            @Override
            public int getServerPort() {
                return serverPort;
            }


            @Override
            public String getUrl() {
                return url;
            }


            @Override
            public String getTempDir() {
                return runnerTempDir;
            }


            @Override
            public void addPropertyChangeListener( final PropertyChangeListener listener ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public void removePropertyChangeListener( final PropertyChangeListener listener ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public OptionState[] getOptions() {
                throw new UnsupportedOperationException();
            }


            @Override
            public OptionState getOption( final String key ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public String getKeyByMethod( final String methodName ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public Object getValueByMethod( final String methodName ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public Properties filterOptions( final Properties properties ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public Map<String, Object> filterOptions( final Map<String, Object> entries ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public void override( final String key, final String override ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public boolean setOverrides( final Overrides overrides ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public Overrides getOverrides() {
                throw new UnsupportedOperationException();
            }


            @Override
            public void bypass( final String key, final String bypass ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public boolean setBypass( final Bypass bypass ) {
                throw new UnsupportedOperationException();
            }


            @Override
            public Bypass getBypass() {
                throw new UnsupportedOperationException();
            }


            @Override
            public Class getFigInterface() {
                return Runner.class;
            }


            @Override
            public boolean isSingleton() {
                return false;
            }


            @Override
            public String toString() {
                return getHostname();
            }
        };
    }
}
