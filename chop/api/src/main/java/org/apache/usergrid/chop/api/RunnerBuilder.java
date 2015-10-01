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
package org.apache.usergrid.chop.api;


import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Properties;

import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.OptionState;
import org.safehaus.guicyfig.Overrides;

import org.apache.commons.lang.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;


/**
 * Builds a Runner for serialization.
 */
public class RunnerBuilder {
    private Properties props;
    private Runner supplied;

    private String ipv4Address;
    private String hostname;
    private int serverPort;
    private String url;
    private String tempDir;


    public RunnerBuilder() {
        // do nothing - supplied will be injected if in Guice env
    }


    public RunnerBuilder( Properties props ) {
        this.props = props;
        updateValues();
    }


    public RunnerBuilder( Runner supplied ) {
        // set the supplied project - this is manually provided
        this.supplied = supplied;
        updateValues();
    }


    public RunnerBuilder setIpv4Address( String ipv4Address ) {
        this.ipv4Address = ipv4Address;
        return this;
    }


    public RunnerBuilder setHostname( String hostname ) {
        this.hostname = hostname;
        return this;
    }


    public RunnerBuilder setServerPort( int serverPort ) {
        this.serverPort = serverPort;
        return this;
    }


    public RunnerBuilder setUrl( String url ) {
        this.url = url;
        return this;
    }


    public RunnerBuilder setTempDir( String tempDir ) {
        this.tempDir = tempDir;
        return this;
    }


    @Inject
    public void setRunner( Runner supplied ) {
        if ( this.supplied == null ) {
            this.supplied = supplied;
            updateValues();
        }
    }


    private void updateValues() {
        if ( supplied != null ) {
            this.ipv4Address = supplied.getIpv4Address();
            this.hostname = supplied.getHostname();
            this.serverPort = supplied.getServerPort();
            this.url = supplied.getUrl();
            this.tempDir = supplied.getTempDir();
        }

        if ( props != null ) {
            if ( props.containsKey( Runner.IPV4_KEY ) ) {
                this.ipv4Address = props.getProperty( Runner.IPV4_KEY );
            }

            if ( props.containsKey( Runner.HOSTNAME_KEY ) ) {
                this.hostname = props.getProperty( Runner.HOSTNAME_KEY );
            }

            if ( props.containsKey( Runner.SERVER_PORT_KEY ) ) {
                this.serverPort = Integer.parseInt( props.getProperty( Runner.SERVER_PORT_KEY ) );
            }

            if ( props.containsKey( Runner.URL_KEY ) ) {
                this.url = props.getProperty( Runner.URL_KEY );
            }

            if ( props.containsKey( Runner.RUNNER_TEMP_DIR_KEY ) ) {
                this.tempDir = props.getProperty( Runner.RUNNER_TEMP_DIR_KEY );
            }
        }
    }


    public Runner getRunner() {
        return new Runner() {
            @JsonProperty
            @Override
            public String getIpv4Address() {
                return ipv4Address;
            }


            @JsonProperty
            @Override
            public String getHostname() {
                return hostname;
            }


            @JsonProperty
            @Override
            public int getServerPort() {
                return serverPort;
            }


            @JsonProperty
            @Override
            public String getUrl() {
                return url;
            }


            @JsonProperty
            @Override
            public String getTempDir() {
                return tempDir;
            }


            @JsonIgnore
            @Override
            public void addPropertyChangeListener( final PropertyChangeListener listener ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public void removePropertyChangeListener( final PropertyChangeListener listener ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public OptionState[] getOptions() {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public OptionState getOption( final String key ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public String getKeyByMethod( final String methodName ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Object getValueByMethod( final String methodName ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Properties filterOptions( final Properties properties ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Map<String, Object> filterOptions( final Map<String, Object> entries ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public void override( final String key, final String override ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public boolean setOverrides( final Overrides overrides ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Overrides getOverrides() {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public void bypass( final String key, final String bypass ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public boolean setBypass( final Bypass bypass ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Bypass getBypass() {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Class getFigInterface() {
                return Project.class;
            }


            @JsonIgnore
            @Override
            public boolean isSingleton() {
                return false;
            }
        };
    }
}
